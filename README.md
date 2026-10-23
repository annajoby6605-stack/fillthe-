# Fill the Pockets – Smart Village Road Reporting & Rewards System

**Fill the Pockets** is a full-stack, college project engineered for Gram Panchayat rural civic management. It empowers villagers to photograph road damage (potholes, asphalt breakdown) and report it with GPS coordinates. AI image verification screens submissions; AI-approved reports then require an authorized Panchayat employee to review them before exactly 20 points are credited. Employees also manage and update road repairs.

---

## 🌐 Localhost Access Links

The following local URLs are available after starting the Spring Boot application and database:

| Portal | Local URL | Description |
|---|---|---|
| **Main Website & Homepage** | **[http://localhost:8080](http://localhost:8080)** | Hero, live statistics, how it works, feature breakdown |
| **Villager Login** | **[http://localhost:8080/villager-login.html](http://localhost:8080/villager-login.html)** | Direct citizen login (No OTP required) |
| **Villager Registration** | **[http://localhost:8080/villager-register.html](http://localhost:8080/villager-register.html)** | Citizen registration |
| **Villager Dashboard** | **[http://localhost:8080/villager-dashboard.html](http://localhost:8080/villager-dashboard.html)** | Personal submissions, repair status, points balance |
| **Report a Road Problem** | **[http://localhost:8080/report-problem.html](http://localhost:8080/report-problem.html)** | Image upload, live GPS capture, instant AI verification |
| **Rewards & Wallet** | **[http://localhost:8080/wallet-rewards.html](http://localhost:8080/wallet-rewards.html)** | Points transaction ledger, coupon redemption store |
| **Panchayat Employee Login** | **[http://localhost:8080/employee-login.html](http://localhost:8080/employee-login.html)** | 2FA verification code modal pop-up login |
| **Employee Dashboard** | **[http://localhost:8080/employee-dashboard.html](http://localhost:8080/employee-dashboard.html)** | Pending image verification, road repair queue, filters and audit updates |
| **Admin Login** | **[http://localhost:8080/admin-login.html](http://localhost:8080/admin-login.html)** | Administrator sign-in |
| **Admin Dashboard** | **[http://localhost:8080/admin-dashboard.html](http://localhost:8080/admin-dashboard.html)** | Administrative overview |

### Report Approval and Points

Images rejected by AI receive `AI_REJECTED` and zero points. AI-approved images are saved as `PENDING_EMPLOYEE_VERIFICATION` and appear in the employee dashboard; submission itself never awards points. An authorized Panchayat employee can approve or reject the report, providing a reason when rejecting. Approval stores `EMPLOYEE_APPROVED` and awards exactly 20 points in the same database transaction; rejection stores `EMPLOYEE_REJECTED` and awards zero points. Villager dashboards display the persisted status, rejection reason, points and current balance.

Uploaded photos are stored in the configured `app.upload.dir` directory (defaults to `uploads`) and their generated filenames are associated with report records. Set `APP_UPLOAD_DIR` to a stable absolute directory when running the application from different working directories. Photos are served by the authenticated `GET /api/reports/{reportId}/image` endpoint; report owners, Panchayat employees and administrators may view them.

To create the first administrator account, set `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD` in the environment before starting the application. The application creates that account if it does not already exist; it does not provide a default administrator password. Keep the password private.

---

## 🏛️ Preconfigured Demo Panchayat Employee Accounts

The database is seeded with 5 demonstration employee accounts:

| Employee ID | Employee Name | Designation | Email Address | Default Password | 2FA Pop-up Code |
|---|---|---|---|---|---|
| `EMP001` | **Arun Kumar** | Executive Officer | `arun.kumar@fillthepockets.demo` | `Demo@Emp123` | Displayed in demo modal |
| `EMP002` | **Anjali Mohan** | Assistant Engineer (Civil) | `anjali.mohan@fillthepockets.demo` | `Demo@Emp123` | Displayed in demo modal |
| `EMP003` | **Rahul Joseph** | Road & Infra Overseer | `rahul.joseph@fillthepockets.demo` | `Demo@Emp123` | Displayed in demo modal |
| `EMP004` | **Meera Thomas** | Panchayat Secretary | `meera.thomas@fillthepockets.demo` | `Demo@Emp123` | Displayed in demo modal |
| `EMP005` | **Nikhil Raj** | Field Works Inspector | `nikhil.raj@fillthepockets.demo` | `Demo@Emp123` | Displayed in demo modal |

> **Note on Demo Mode:** When logging into the Panchayat Employee portal, enter one of the demo emails and `Demo@Emp123`. The system verifies the employee credentials and launches the **verification code pop-up modal**. In college demo mode, the 6-digit code is clearly displayed in a dedicated demo banner and automatically prefilled.

---

## 🛠️ Technology Stack

- **Frontend:** Responsive HTML5, CSS3, Vanilla JavaScript (Forest Green, Cream, White, and Gold theme).
- **Backend:** Java 17 with Spring Boot 3.2.5 (Spring Web, Spring Security, Spring Data JPA, JWT).
- **Database:** MySQL / MariaDB (running on `localhost:3306`, database `fill_the_pockets`).
- **AI Verification:** Microsoft ONNX Runtime (`mobilenetv2-7.onnx`) combined with Computer Vision surface gradient and texture analysis.
- **Duplicate Prevention:** Perceptual Image Difference Hashing (dHash 64-bit) + Haversine GPS proximity (15-meter radius).

---

## 🧪 Automated Tests

Run `mvn test` to execute the available automated tests. They cover report upload classification, employee approval/rejection behavior, duplicate-approval protection, and other project workflows. These tests do not replace a live browser/database end-to-end test.

---

## 🚀 How to Run Locally in VS Code (Manual Restart Instructions)

If you ever restart your computer or stop the background processes, run these simple steps:

### 1. Start MySQL Server:
```powershell
.\start_mysql.ps1
```

### 2. Start Spring Boot Application:
```powershell
$toolsDir = Join-Path $env:USERPROFILE ".tools"
$env:JAVA_HOME = Join-Path $toolsDir "jdk-17"
$mavenBin = Join-Path $toolsDir "maven\bin"
$env:Path = "$env:JAVA_HOME\bin;$mavenBin;$env:Path"
java -jar target\fill-the-pockets-1.0.0.jar
```

### 3. Open in Browser:
Open [http://localhost:8080](http://localhost:8080) in Chrome, Edge, or Firefox.

---

## 📤 Sharing with Your Teacher / Future Public Deployment

To demonstrate this website to your teacher or evaluator online without paying for hosting:

### Option A: Free Instant Tunnel (No deployment required, 2 minutes)
Keep the website running locally on your computer and generate a secure temporary public URL using **Cloudflare Tunnel** or **LocalTunnel**:
```powershell
# Using npx localtunnel (free, no sign-up)
npx localtunnel --port 8080
```
This gives you an instant public HTTPS URL (e.g. `https://smart-village-roads.loca.lt`) that your teacher can open on their phone or laptop.

### Option B: Cloud Hosting (Production)
1. **Database:** Deploy MySQL on Railway, Aiven, or Supabase.
2. **Backend:** Deploy the Spring Boot JAR to Render, Railway, or AWS Elastic Beanstalk. Set environment variables `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`.
3. Set `app.demo.mode=false` to switch to real SMS/Email OTP delivery.
