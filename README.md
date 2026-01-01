# FitFamily Backend

A comprehensive Spring Boot REST API backend for the FitFamily application - a family-oriented food tracking and nutrition management platform.

## 📋 Overview

FitFamily Backend provides a secure, scalable API for managing user authentication, family groups, food databases, meal logging, and personalized nutrition dashboards. Built with Spring Boot 3.x and designed for production deployment with PostgreSQL.

## ✨ Features

### 🔐 Authentication & Authorization
- User registration and login with JWT tokens
- Secure password encryption (BCrypt)
- Role-based access control (ADMIN, MEMBER)
- Stateless session management

### 👨‍👩‍👧‍👦 Family Management
- Create and join family groups
- Unique join codes for family invitations
- Role assignment (Admin/Member)
- Family-wide nutrition tracking

### 🍎 Food Database
- Comprehensive food catalog with nutritional information
- Search foods by name (case-insensitive)
- Pre-loaded sample data (seeding)
- Multiple portion sizes per food item

### 📊 Meal Logging
- Log meals with specific portions
- Meal type categorization (Breakfast, Lunch, Dinner, Snack)
- Automatic macro calculation (calories, protein, carbs, fat)
- Date-based meal tracking

### 📈 Nutrition Dashboard
- Personal daily macro summaries
- Family member dashboard views
- Meal history by date
- Aggregated nutrition statistics

### 🏥 Health & Monitoring
- Health check endpoints for load balancers
- Database connectivity monitoring
- Readiness and liveness probes (Kubernetes-ready)
- Spring Boot Actuator integration

### 🌐 Security & CORS
- Environment-specific CORS configuration
- Security headers (XSS, Content-Type, Frame Options)
- Production-hardened security settings
- Secure error handling (no stack traces in production)

## 🛠️ Technology Stack

### Core Framework
- **Spring Boot**: 3.5.9
- **Java**: 17
- **Maven**: 3.x

### Spring Modules
- Spring Web (REST API)
- Spring Data JPA (ORM)
- Spring Security (Authentication & Authorization)
- Spring Boot Actuator (Monitoring)
- Spring Validation

### Database
- **PostgreSQL**: 15+ (Production)
- **H2**: In-memory (Development/Testing)
- **HikariCP**: Connection pooling

### Security
- **JWT**: JSON Web Tokens (io.jsonwebtoken:jjwt 0.12.3)
- **BCrypt**: Password hashing

### Other
- **Lombok**: Boilerplate reduction
- **JUnit 5 & Mockito**: Testing

## 📦 Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **PostgreSQL 15+** (for production)
- **Git**

Optional:
- Docker & Docker Compose (for containerized deployment)
- IDE: IntelliJ IDEA, Eclipse, or VS Code

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd fitfamily-backend
```

### 2. Build the Project

```bash
# Build with tests
mvn clean package

# Build without tests (faster)
mvn clean package -DskipTests
```

### 3. Run with Development Profile (H2 Database)

```bash
# Using Maven
mvn spring-boot:run

# Or using the JAR
java -jar target/fitfamily-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

The application will start on `http://localhost:8080`

### 4. Verify Installation

```bash
# Simple health check
curl http://localhost:8080/health

# Detailed health check
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "application": "FitFamily Backend",
  "message": "Application is running"
}
```

## ⚙️ Configuration

### Environment Profiles

The application supports two profiles:

#### Development (`dev`)
- Uses H2 in-memory database
- Verbose logging
- H2 console enabled at `/h2-console`
- Auto-creates database schema
- CORS allows multiple localhost ports

**No configuration required** - works out of the box!

#### Production (`prod`)
- Uses PostgreSQL database
- Minimal logging (WARN level)
- Schema validation only (no auto-creation)
- Strict CORS policy
- Requires environment variables

### Environment Variables (Production)

Create a `.env` file or export these variables:

```bash
# Required
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://localhost:5432/fitfamily
DB_USERNAME=fitfamily
DB_PASSWORD=your-secure-password
JWT_SECRET=your-64-character-hex-secret
CORS_ALLOWED_ORIGINS=https://app.yourdomain.com

# Optional
SERVER_PORT=8080
JWT_EXPIRATION=3600000  # 1 hour in milliseconds
DB_POOL_SIZE=10
```

### Generate Secure JWT Secret

```bash
# Generate a 32-byte (64 character) hex secret
openssl rand -hex 32
```

## 🗄️ Database Setup

### Development (H2)
No setup required - database is created automatically in memory.

### Production (PostgreSQL)

#### 1. Create Database and User

```bash
# Connect to PostgreSQL
sudo -u postgres psql

# Create database and user
CREATE DATABASE fitfamily;
CREATE USER fitfamily WITH PASSWORD 'your-password';
GRANT ALL PRIVILEGES ON DATABASE fitfamily TO fitfamily;
\q
```

#### 2. Initialize Schema

```bash
# Option 1: Use provided SQL script
psql -U fitfamily -d fitfamily -f postgresql-setup.sql

# Option 2: Let Hibernate create schema (first run only)
# Set in application-prod.yml:
# spring.jpa.hibernate.ddl-auto: update
```

#### 3. Verify Connection

```bash
psql -U fitfamily -d fitfamily -c "\dt"
```

You should see tables: `users`, `families`, `foods`, `food_portions`, `food_logs`

## 🔌 API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/auth/register` | Register new user | No |
| POST | `/auth/login` | Login and get JWT token | No |

### Family Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/families/create` | Create a new family | Yes |
| POST | `/families/join` | Join existing family | Yes |
| GET | `/families/my-family` | Get current user's family | Yes |

### Food Search

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/foods/search?query={name}` | Search foods by name | Yes |

### Meal Logging

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/food-logs` | Log a meal | Yes |
| DELETE | `/food-logs/{id}` | Delete a meal log | Yes |

### Dashboard

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/dashboard/daily?date={YYYY-MM-DD}` | Get personal daily dashboard | Yes |
| GET | `/dashboard/family?date={YYYY-MM-DD}` | Get family dashboard | Yes |

### Health & Monitoring

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/health` | Simple health check | No |
| GET | `/actuator/health` | Detailed health status | No |
| GET | `/actuator/health/readiness` | Readiness probe | No |
| GET | `/actuator/health/liveness` | Liveness probe | No |

## 📝 API Examples

### Register a User

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "john@example.com",
  "name": "John Doe"
}
```

### Search Foods (Authenticated)

```bash
TOKEN="your-jwt-token"

curl -X GET "http://localhost:8080/foods/search?query=chicken" \
  -H "Authorization: Bearer $TOKEN"
```

### Log a Meal

```bash
curl -X POST http://localhost:8080/food-logs \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "foodId": "food-uuid",
    "portionId": "portion-uuid",
    "mealType": "LUNCH"
  }'
```

### Get Daily Dashboard

```bash
curl -X GET "http://localhost:8080/dashboard/daily?date=2026-01-01" \
  -H "Authorization: Bearer $TOKEN"
```

## 🧪 Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=AuthServiceTest
```

### Run Integration Tests

```bash
mvn test -Dtest=*IT
```

### Test Coverage

The project includes:
- **Unit Tests**: Service layer logic testing with Mockito
- **Integration Tests**: End-to-end API testing with MockMvc
- **Test Classes**: 11 test classes covering controllers and services

Test files:
- `AuthServiceTest` - Authentication logic
- `FamilyServiceTest` - Family management
- `FoodServiceTest` - Food search
- `FoodLogServiceTest` - Meal logging
- `DashboardServiceTest` - Dashboard calculations
- `AuthControllerIT` - Auth API endpoints
- `FamilyControllerIT` - Family API endpoints
- `FoodControllerIT` - Food API endpoints
- `FoodLogControllerIT` - Food log API endpoints
- `DashboardControllerIT` - Dashboard API endpoints

## 🐳 Docker Deployment

### Using Docker Compose

```bash
# Set environment variables
export DB_PASSWORD="your-password"
export JWT_SECRET="your-secret"

# Start all services (PostgreSQL + Backend)
docker-compose up -d

# View logs
docker-compose logs -f backend

# Stop services
docker-compose down
```

### Build Docker Image

```bash
docker build -t fitfamily-backend:latest .
```

### Run Container

```bash
docker run -d \
  --name fitfamily-backend \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://db:5432/fitfamily \
  -e DB_USERNAME=fitfamily \
  -e DB_PASSWORD=your-password \
  -e JWT_SECRET=your-secret \
  -e CORS_ALLOWED_ORIGINS=https://yourdomain.com \
  fitfamily-backend:latest
```

## 🚢 Production Deployment

### 1. Set Environment Variables

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:postgresql://your-db-host:5432/fitfamily
export DB_USERNAME=fitfamily
export DB_PASSWORD=your-secure-password
export JWT_SECRET=$(openssl rand -hex 32)
export CORS_ALLOWED_ORIGINS=https://app.yourdomain.com
```

### 2. Run Application

```bash
# With explicit memory settings
java -Xms512m -Xmx1024m \
     -jar target/fitfamily-backend-0.0.1-SNAPSHOT.jar
```

### 3. Verify Deployment

```bash
# Check health
curl https://api.yourdomain.com/actuator/health

# Check database connectivity
curl https://api.yourdomain.com/actuator/health | jq '.components.db'
```

### Production Checklist

- [ ] PostgreSQL database created and configured
- [ ] All environment variables set
- [ ] JWT secret is secure (32+ characters)
- [ ] CORS origins set to production URLs (not localhost)
- [ ] Database schema initialized
- [ ] SSL/HTTPS configured (via reverse proxy)
- [ ] Firewall rules configured
- [ ] Health checks tested
- [ ] Monitoring/logging configured
- [ ] Backup strategy in place

## 📁 Project Structure

```
fitfamily-backend/
├── src/
│   ├── main/
│   │   ├── java/com/fitfamily/app/
│   │   │   ├── config/              # Configuration classes
│   │   │   │   ├── DataSeeder.java
│   │   │   │   └── PasswordEncoderConfig.java
│   │   │   ├── controller/          # REST Controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── FamilyController.java
│   │   │   │   ├── FoodController.java
│   │   │   │   ├── FoodLogController.java
│   │   │   │   ├── DashboardController.java
│   │   │   │   └── HealthCheckController.java
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── exception/           # Custom Exceptions & Global Handler
│   │   │   ├── model/               # JPA Entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Family.java
│   │   │   │   ├── Food.java
│   │   │   │   ├── FoodPortion.java
│   │   │   │   ├── FoodLog.java
│   │   │   │   └── Role.java, MealType.java
│   │   │   ├── repository/          # Spring Data JPA Repositories
│   │   │   ├── security/            # Security Configuration
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtUtil.java
│   │   │   │   └── JwtAuthenticationFilter.java
│   │   │   ├── service/             # Business Logic
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── FamilyService.java
│   │   │   │   ├── FoodService.java
│   │   │   │   ├── FoodLogService.java
│   │   │   │   └── DashboardService.java
│   │   │   ├── util/                # Utility Classes
│   │   │   └── FitFamilyApplication.java
│   │   └── resources/
│   │       ├── application.yaml           # Common configuration
│   │       ├── application-dev.yml        # Development profile
│   │       └── application-prod.yml       # Production profile
│   └── test/                        # Test classes
├── target/                          # Build output
├── pom.xml                         # Maven configuration
├── Dockerfile                      # Docker image definition
├── docker-compose.yml              # Docker Compose configuration
├── .dockerignore                   # Docker ignore rules
├── .gitignore                      # Git ignore rules
├── postgresql-setup.sql            # Database initialization script
└── README.md                       # This file
```

## 🔧 Configuration Files

### application.yaml
Common configuration shared across all profiles (server port, JWT settings, Actuator).

### application-dev.yml
Development-specific settings:
- H2 in-memory database
- Verbose logging (DEBUG level)
- Auto schema creation
- H2 console enabled
- Relaxed CORS policy

### application-prod.yml
Production-specific settings:
- PostgreSQL database
- Minimal logging (WARN level)
- Schema validation only
- Optimized connection pooling
- Strict CORS policy
- Security hardening

## 🛡️ Security Features

- **JWT Authentication**: Stateless token-based authentication
- **Password Encryption**: BCrypt hashing with salt
- **CORS Configuration**: Environment-specific allowed origins
- **Security Headers**: XSS protection, Content-Type, Frame Options
- **SQL Injection Prevention**: JPA parameterized queries
- **Session Management**: Stateless (no server-side sessions)
- **Error Handling**: No stack traces exposed in production
- **Input Validation**: Bean Validation (JSR-380)

## 📊 Monitoring & Health Checks

### Health Check Endpoints

- **Simple Health**: `GET /health`
- **Detailed Health**: `GET /actuator/health`
- **Readiness Probe**: `GET /actuator/health/readiness`
- **Liveness Probe**: `GET /actuator/health/liveness`

### What's Monitored

- Application status
- Database connectivity (PostgreSQL/H2)
- Disk space availability
- Readiness state (ready to serve traffic)
- Liveness state (application not deadlocked)

### Kubernetes Probes

The health endpoints are designed for Kubernetes deployments:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
```

## 🐛 Troubleshooting

### Common Issues

#### Application won't start - Missing environment variables
**Error**: `Could not resolve placeholder 'DB_URL'`
**Solution**: Set all required environment variables for production profile

#### Database connection failed
**Error**: `Connection refused` or `password authentication failed`
**Solution**: 
- Verify PostgreSQL is running: `sudo systemctl status postgresql`
- Test connection: `psql -U fitfamily -h localhost -d fitfamily`
- Check credentials in environment variables

#### Port already in use
**Error**: `Port 8080 was already in use`
**Solution**: 
```bash
# Find and kill process
lsof -ti:8080 | xargs kill -9
# Or use different port
export SERVER_PORT=8081
```

#### Schema validation failed
**Error**: `Schema-validation: missing table [users]`
**Solution**: Initialize database schema using `postgresql-setup.sql`

### Enable Debug Logging

```bash
# In application-dev.yml or as environment variable
logging.level.com.fitfamily.app=DEBUG
```

### View Application Logs

```bash
# If running with Maven
mvn spring-boot:run

# If running as JAR
java -jar target/fitfamily-backend-0.0.1-SNAPSHOT.jar

# Docker logs
docker logs -f fitfamily-backend
```

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow Java coding conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Write unit tests for new features
- Ensure all tests pass before submitting PR

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Authors

- **Development Team** - Initial work

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- PostgreSQL community
- All contributors to the dependencies used in this project

## 📞 Support

For issues, questions, or contributions:
- Create an issue in the repository
- Contact the development team

---

**Version**: 0.0.1-SNAPSHOT  
**Last Updated**: January 1, 2026  
**Spring Boot**: 3.5.9  
**Java**: 17  

---

Made with ❤️ by the FitFamily Team

