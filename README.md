# BigProject

## Run with database

The app will try to connect to the database as soon as `mvn javafx:run` starts.

Configuration priority:
1. Environment variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`
2. Local file: `.env.local`
3. Built-in defaults for local development

Create `.env.local` from the example:

```bash
cp .env.local.example .env.local
```

Then fill in your database credentials and run:

```bash
mvn javafx:run
```
