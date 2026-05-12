# BigProject
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Git](https://img.shields.io/badge/git-%23F05033.svg?style=for-the-badge&logo=git&logoColor=white)

![Static Badge](https://img.shields.io/badge/coverage-95%25-orange)
![Maintenance](https://img.shields.io/badge/Maintainance-yes-green.svg)
[![Static Badge](https://img.shields.io/badge/github-repo-blue?logo=github)](https://github.com/25020073-ZUNN/BigProject)
![Generic badge](https://img.shields.io/badge/BUILD-PASSING-<COLOR>.svg)
![GitHub license](https://img.shields.io/github/license/Naereen/StrapDown.js.svg)
![Static Badge](https://img.shields.io/badge/star-1-yellow)

![Static Badge](https://img.shields.io/badge/Project_Manager-Tan_Dung-indigo)
![Static Badge](https://img.shields.io/badge/contributors-Trung_Dung-pink)
![Static Badge](https://img.shields.io/badge/UX/contributors-Van_Thuyet-purple)
![Static Badge](https://img.shields.io/badge/contributors-Pham_Trung-aqua)

[![GitHub watchers](https://img.shields.io/github/watchers/Naereen/StrapDown.js.svg?style=social&label=Watch&maxAge=2592000)](https://github.com/25020073-ZUNN/BigProject/watchers)
[![GitHub followers](https://img.shields.io/github/followers/Naereen.svg?style=social&label=Follow&maxAge=2592000)](https://github.com/Naereen?tab=followers)



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
