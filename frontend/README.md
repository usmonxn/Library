# BookVault Frontend

Static frontend for the local microservices.

Ports:

- Auth service: `http://localhost:8092`
- User service: `http://localhost:8090`
- Book service: `http://localhost:8084`

Run from this folder:

```bash
python3 -m http.server 3000
```

Open (any of these work):

```text
http://localhost:3000
http://localhost:63342   (IntelliJ / WebStorm built-in server)
```

Backend CORS allows all `localhost` ports for development.
