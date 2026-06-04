package com.example.userservice.service;

import com.example.userservice.entity.BorrowsEntity;
import com.example.userservice.entity.AuditLogEntity;
import com.example.userservice.entity.LibraryNotificationEntity;
import com.example.userservice.exception.CustomException;
import com.example.userservice.repository.AuditLogRepository;
import com.example.userservice.repository.BorrowsRepository;
import com.example.userservice.repository.LibraryNotificationRepository;
import com.example.userservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class BorrowsService {
    private static final int DAILY_FINE_SUM = 20000;
    private static final int MAX_ACTIVE_BORROWS = 3;
    private static final String STATUS_BORROWED = "BORROWED";
    private static final String STATUS_RETURN_REVIEW = "RETURN_REVIEW";
    private static final String STATUS_FINE_PENDING = "FINE_PENDING";
    private static final String STATUS_RETURNED = "RETURNED";

    private final BorrowsRepository borrowsRepository;
    private final BookInventoryClient bookInventoryClient;
    private final AuditLogRepository auditLogRepository;
    private final LibraryNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationEmailService notificationEmailService;

    public void createBorrow(BorrowsEntity borrowsEntity) {
        validatePayment(borrowsEntity);
        long activeCount = borrowsRepository.countByUserIdAndStatus(borrowsEntity.getUserId(), STATUS_BORROWED)
                + borrowsRepository.countByUserIdAndStatus(borrowsEntity.getUserId(), STATUS_RETURN_REVIEW)
                + borrowsRepository.countByUserIdAndStatus(borrowsEntity.getUserId(), STATUS_FINE_PENDING);
        if (activeCount >= MAX_ACTIVE_BORROWS) {
            throw new CustomException(400, "Bir foydalanuvchi bir vaqtda maksimum 3 ta kitob olishi mumkin");
        }
        if (hasOverdueBorrow(borrowsEntity.getUserId())) {
            throw new CustomException(400, "Muddati o'tgan kitob bor. Avval uni qaytaring va jarimani to'lang");
        }
        if (borrowsRepository.existsByUserIdAndBookIdAndStatus(borrowsEntity.getUserId(), borrowsEntity.getBookId(), STATUS_BORROWED)
                || borrowsRepository.existsByUserIdAndBookIdAndStatus(borrowsEntity.getUserId(), borrowsEntity.getBookId(), STATUS_RETURN_REVIEW)
                || borrowsRepository.existsByUserIdAndBookIdAndStatus(borrowsEntity.getUserId(), borrowsEntity.getBookId(), STATUS_FINE_PENDING)) {
            throw new CustomException(400, "Bu kitob foydalanuvchida allaqachon bor");
        }

        bookInventoryClient.borrowCopy(borrowsEntity.getBookId());
        LocalDate now = LocalDate.now();
        borrowsEntity.setBorrowDate(String.valueOf(now));
        borrowsEntity.setDueDate(String.valueOf(now.plusDays(borrowsEntity.getRentalDays())));
        borrowsEntity.setOverdueDays(0);
        borrowsEntity.setFineSum(0);
        borrowsEntity.setPaymentStatus("PAID");
        borrowsEntity.setPaidSum(borrowsEntity.getPriceSum());
        borrowsEntity.setStatus(STATUS_BORROWED);
        BorrowsEntity saved = borrowsRepository.save(borrowsEntity);
        audit("BORROW_CREATED", saved, "Ijara yaratildi. Tarif: " + saved.getRentalPlan());
        if (notify("BORROW_CREATED", saved, "Yangi ijara yaratildi. User #" + saved.getUserId() + ", kitob #" + saved.getBookId())) {
            userRepository.findById(saved.getUserId()).ifPresent(user -> notificationEmailService.borrowCreated(user, saved));
        }
    }

    public List<BorrowsEntity> getMyBooks(Long userId) {
        return borrowsRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<BorrowsEntity> getAllBorrows() {
        List<BorrowsEntity> borrows = borrowsRepository.findAll();
        borrows.forEach(borrow -> {
            if (STATUS_FINE_PENDING.equals(borrow.getStatus())) {
                applyFine(borrow, LocalDate.now());
                borrowsRepository.save(borrow);
            }
        });
        return borrows;
    }

    public List<BorrowsEntity> getReturnReviewBorrows() {
        refreshDueNotifications();
        return borrowsRepository.findByStatusOrderByCreatedAtDesc(STATUS_RETURN_REVIEW);
    }

    public void returnBorrow(Long borrowId, Long requesterUserId) {
        BorrowsEntity borrow = borrowsRepository.findById(borrowId)
                .orElseThrow(() -> new CustomException(404, "Ijara topilmadi"));
        if (requesterUserId != null && !requesterUserId.equals(borrow.getUserId())) {
            throw new CustomException(403, "Faqat o'zingiz olgan kitobni qaytarishingiz mumkin");
        }
        if (STATUS_RETURNED.equals(borrow.getStatus())) {
            throw new CustomException(400, "Bu kitob oldin qaytarilgan");
        }
        LocalDate returnDate = LocalDate.now();
        if (STATUS_FINE_PENDING.equals(borrow.getStatus())) {
            applyFine(borrow, returnDate);
        } else if (!STATUS_RETURN_REVIEW.equals(borrow.getStatus())) {
            applyFine(borrow, returnDate);
        }
        finishReturn(borrow, returnDate, "BORROW_RETURNED", "Kitob qaytdi");
    }

    public void confirmReturnedNoFine(Long borrowId) {
        BorrowsEntity borrow = borrowsRepository.findById(borrowId)
                .orElseThrow(() -> new CustomException(404, "Ijara topilmadi"));
        if (!STATUS_RETURN_REVIEW.equals(borrow.getStatus())) {
            throw new CustomException(400, "Bu ijara admin tekshiruvida emas");
        }
        borrow.setOverdueDays(0);
        borrow.setFineSum(0);
        finishReturn(borrow, LocalDate.now(), "RETURN_CONFIRMED_NO_FINE", "Admin tasdiqladi: kitob qaytgan, jarima yo'q");
    }

    public void sendToFine(Long borrowId) {
        BorrowsEntity borrow = borrowsRepository.findById(borrowId)
                .orElseThrow(() -> new CustomException(404, "Ijara topilmadi"));
        if (!STATUS_RETURN_REVIEW.equals(borrow.getStatus())) {
            throw new CustomException(400, "Bu ijara admin tekshiruvida emas");
        }
        borrow.setStatus(STATUS_FINE_PENDING);
        applyFine(borrow, LocalDate.now());
        borrowsRepository.save(borrow);
        audit("RETURN_REJECTED_FINE_STARTED", borrow, "Admin rad etdi: jarima boshlandi. Jarima: " + borrow.getFineSum());
        if (notify("FINE_STARTED", borrow, "Kitob #" + borrow.getBookId() + " qaytmagan deb belgilandi. Jarima: " + borrow.getFineSum() + " so'm")) {
            userRepository.findById(borrow.getUserId()).ifPresent(user -> notificationEmailService.fineStarted(user, borrow));
        }
    }

    public List<LibraryNotificationEntity> getNotifications() {
        refreshDueNotifications();
        return notificationRepository.findTop20ByOrderByCreatedAtDesc();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void refreshDueNotificationsSchedule() {
        refreshDueNotifications();
    }

    public List<AuditLogEntity> getAuditLogs() {
        return auditLogRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public Map<String, Object> getAnalytics() {
        List<BorrowsEntity> borrows = getAllBorrows();
        long active = borrows.stream().filter(b -> STATUS_BORROWED.equals(b.getStatus()) || STATUS_FINE_PENDING.equals(b.getStatus())).count();
        long completed = borrows.stream().filter(b -> STATUS_RETURNED.equals(b.getStatus())).count();
        long overdue = borrows.stream().filter(b -> STATUS_FINE_PENDING.equals(b.getStatus())).count();
        long returnReview = borrows.stream().filter(b -> STATUS_RETURN_REVIEW.equals(b.getStatus())).count();
        long dueTomorrow = borrows.stream().filter(this::isDueTomorrow).count();
        long todayBorrows = borrows.stream().filter(b -> String.valueOf(LocalDate.now()).equals(b.getBorrowDate())).count();
        int revenue = borrows.stream().mapToInt(b -> b.getPaidSum() == null ? 0 : b.getPaidSum()).sum();
        int fineRevenue = borrows.stream().mapToInt(b -> b.getFineSum() == null ? 0 : b.getFineSum()).sum();
        int todayRevenue = borrows.stream()
                .filter(b -> String.valueOf(LocalDate.now()).equals(b.getBorrowDate()))
                .mapToInt(b -> b.getPaidSum() == null ? 0 : b.getPaidSum())
                .sum();
        Map<String, Long> weeklyLending = weeklyLendingActivity(borrows);
        Map<Long, Long> popularBooks = new LinkedHashMap<>();
        borrows.stream()
                .collect(java.util.stream.Collectors.groupingBy(BorrowsEntity::getBookId, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> popularBooks.put(e.getKey(), e.getValue()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeBorrows", active);
        result.put("completedBorrows", completed);
        result.put("overdueBorrows", overdue);
        result.put("returnReviewBorrows", returnReview);
        result.put("dueTomorrowBorrows", dueTomorrow);
        result.put("todayBorrows", todayBorrows);
        result.put("totalUsers", userRepository.count());
        result.put("revenue", revenue);
        result.put("fineRevenue", fineRevenue);
        result.put("todayRevenue", todayRevenue);
        result.put("averagePaid", borrows.isEmpty() ? 0 : revenue / borrows.size());
        result.put("popularBooks", popularBooks);
        result.put("weeklyLending", weeklyLending);
        result.put("maxActiveBorrows", MAX_ACTIVE_BORROWS);
        result.put("dailyFineSum", DAILY_FINE_SUM);
        return result;
    }

    private Map<String, Long> weeklyLendingActivity(List<BorrowsEntity> borrows) {
        String[] labels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        Map<String, Long> weekly = new LinkedHashMap<>();
        for (int i = 0; i < labels.length; i++) {
            String date = String.valueOf(weekStart.plusDays(i));
            long count = borrows.stream()
                    .filter(b -> date.equals(b.getBorrowDate()))
                    .count();
            weekly.put(labels[i], count);
        }
        return weekly;
    }

    private void applyFine(BorrowsEntity borrow, LocalDate returnDate) {
        if (borrow.getDueDate() == null || borrow.getDueDate().isBlank()) {
            borrow.setOverdueDays(0);
            borrow.setFineSum(0);
            return;
        }
        LocalDate dueDate = LocalDate.parse(borrow.getDueDate());
        int overdueDays = Math.max(0, (int) ChronoUnit.DAYS.between(dueDate, returnDate));
        borrow.setOverdueDays(overdueDays);
        borrow.setFineSum(overdueDays * DAILY_FINE_SUM);
        if (STATUS_FINE_PENDING.equals(borrow.getStatus()) && overdueDays > 0) {
            borrow.setPaymentStatus("FINE_PENDING");
        }
    }

    private void validatePayment(BorrowsEntity borrowsEntity) {
        if (borrowsEntity.getRentalPlan() == null || borrowsEntity.getRentalPlan().isBlank()) {
            throw new CustomException(400, "Ijara tarifi tanlanishi kerak");
        }
        switch (borrowsEntity.getRentalPlan()) {
            case "ONE_DAY" -> {
                borrowsEntity.setRentalDays(1);
                borrowsEntity.setPriceSum(35000);
            }
            case "ONE_WEEK" -> {
                borrowsEntity.setRentalDays(7);
                borrowsEntity.setPriceSum(70000);
            }
            case "ONE_MONTH" -> {
                borrowsEntity.setRentalDays(30);
                borrowsEntity.setPriceSum(250000);
            }
            default -> throw new CustomException(400, "Noto'g'ri ijara tarifi");
        }
        if (borrowsEntity.getCardLastFour() == null || !borrowsEntity.getCardLastFour().matches("\\d{4}")) {
            throw new CustomException(400, "Karta oxirgi 4 raqami kiritilishi kerak");
        }
        if (borrowsEntity.getCardHolder() == null || borrowsEntity.getCardHolder().isBlank()) {
            throw new CustomException(400, "Karta egasi ism-familiyasi kiritilishi kerak");
        }
        if (borrowsEntity.getCardExpiry() == null || !borrowsEntity.getCardExpiry().matches("(0[1-9]|1[0-2])/\\d{2}")) {
            throw new CustomException(400, "Karta muddati MM/YY formatida bo'lishi kerak");
        }
    }

    private boolean hasOverdueBorrow(Long userId) {
        return borrowsRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .anyMatch(b -> STATUS_FINE_PENDING.equals(b.getStatus()));
    }

    private boolean isDueTomorrow(BorrowsEntity borrow) {
        if (borrow.getDueDate() == null || borrow.getDueDate().isBlank()) return false;
        return LocalDate.parse(borrow.getDueDate()).equals(LocalDate.now().plusDays(1));
    }

    private void refreshDueNotifications() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        for (BorrowsEntity borrow : borrowsRepository.findByStatus(STATUS_BORROWED)) {
            if (borrow.getDueDate() == null || borrow.getDueDate().isBlank()) continue;
            LocalDate due = LocalDate.parse(borrow.getDueDate());
            if (due.equals(tomorrow)) {
                if (notify("DUE_TOMORROW", borrow, "Kitob #" + borrow.getBookId() + " qaytarishiga 1 kun qoldi")) {
                    userRepository.findById(borrow.getUserId()).ifPresent(user -> notificationEmailService.dueTomorrow(user, borrow));
                }
            } else if (due.isBefore(LocalDate.now())) {
                borrow.setStatus(STATUS_RETURN_REVIEW);
                borrow.setPaymentStatus("RETURN_CHECK");
                borrow.setOverdueDays(0);
                borrow.setFineSum(0);
                borrowsRepository.save(borrow);
                notify("RETURN_REVIEW", borrow, "Kitob #" + borrow.getBookId() + " muddati tugadi. Admin qaytganini tekshirishi kerak");
            }
        }
        for (BorrowsEntity borrow : borrowsRepository.findByStatus(STATUS_FINE_PENDING)) {
            applyFine(borrow, LocalDate.now());
            borrowsRepository.save(borrow);
            if (notify("OVERDUE", borrow, "Kitob #" + borrow.getBookId() + " jarimada. Jarima: " + borrow.getFineSum() + " so'm")) {
                userRepository.findById(borrow.getUserId()).ifPresent(user -> notificationEmailService.fineStarted(user, borrow));
            }
        }
    }

    private void finishReturn(BorrowsEntity borrow, LocalDate returnDate, String auditAction, String auditDetails) {
        borrow.setStatus(STATUS_RETURNED);
        borrow.setReturnDate(String.valueOf(returnDate));
        borrow.setPaymentStatus("PAID");
        borrow.setPaidSum((borrow.getPriceSum() == null ? 0 : borrow.getPriceSum()) + (borrow.getFineSum() == null ? 0 : borrow.getFineSum()));
        borrowsRepository.save(borrow);
        bookInventoryClient.returnCopy(borrow.getBookId());
        audit(auditAction, borrow, auditDetails + ". Jami to'lov: " + borrow.getPaidSum());
        if (notify("BORROW_RETURNED", borrow, "Kitob qaytarildi. Jarima: " + borrow.getFineSum() + " so'm")) {
            userRepository.findById(borrow.getUserId()).ifPresent(user -> notificationEmailService.returned(user, borrow));
        }
    }

    private void audit(String action, BorrowsEntity borrow, String details) {
        AuditLogEntity audit = new AuditLogEntity();
        audit.setAction(action);
        audit.setUserId(borrow.getUserId());
        audit.setBookId(borrow.getBookId());
        audit.setBorrowId(borrow.getId());
        audit.setDetails(details);
        auditLogRepository.save(audit);
    }

    private boolean notify(String type, BorrowsEntity borrow, String message) {
        if (borrow.getId() != null && notificationRepository.existsByTypeAndBorrowId(type, borrow.getId())) {
            return false;
        }
        LibraryNotificationEntity notification = new LibraryNotificationEntity();
        notification.setType(type);
        notification.setUserId(borrow.getUserId());
        notification.setBookId(borrow.getBookId());
        notification.setBorrowId(borrow.getId());
        notification.setMessage(message);
        notification.setRead(false);
        notificationRepository.save(notification);
        return true;
    }
}
