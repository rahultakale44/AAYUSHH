# MongoDB Setup Guide for AAYUSH Backend

## Quick Start (Choose One Option)

### Option 1: Docker Compose (Recommended) ⭐

**Prerequisites**: Docker Desktop installed and running

```bash
# Navigate to backend directory
cd backend

# Start MongoDB
docker-compose up -d

# Verify MongoDB is running
docker-compose ps

# View MongoDB logs
docker-compose logs -f mongodb

# Stop MongoDB
docker-compose down
```

**Advantages**:
- ✅ No local installation required
- ✅ Isolated environment
- ✅ Easy to start/stop
- ✅ Consistent across team members

---

### Option 2: Local MongoDB Installation

#### Windows Installation

1. **Download MongoDB Community Server**
   - Visit: https://www.mongodb.com/try/download/community
   - Select: Windows x64
   - Click: Download

2. **Install MongoDB**
   - Run the MSI installer
   - Choose "Complete" installation
   - Install as Windows Service (recommended)
   - Install MongoDB Compass (optional GUI tool)

3. **Verify Installation**
   ```bash
   # Check MongoDB service status
   sc query MongoDB
   
   # Connect to MongoDB shell
   mongosh
   ```

4. **Create Database** (Optional)
   ```javascript
   // In mongosh
   use aayush_db
   db.createCollection("emergency_sessions")
   show dbs
   ```

#### Mac/Linux Installation

```bash
# macOS (using Homebrew)
brew tap mongodb/brew
brew install mongodb-community@7.0
brew services start mongodb-community@7.0

# Ubuntu/Debian
wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu $(lsb_release -cs)/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
sudo apt-get update
sudo apt-get install -y mongodb-org
sudo systemctl start mongod
```

---

## Running the Backend

### Step 1: Set Environment Variables

**Windows PowerShell**:
```powershell
$env:MONGODB_URI="mongodb://localhost:27017/aayush_db"
```

**Windows CMD**:
```cmd
set MONGODB_URI=mongodb://localhost:27017/aayush_db
```

**Mac/Linux**:
```bash
export MONGODB_URI="mongodb://localhost:27017/aayush_db"
```

### Step 2: Start Application

```bash
# Navigate to backend directory
cd backend

# Run application
./mvnw spring-boot:run

# Or on Windows
mvnw.cmd spring-boot:run
```

### Expected Output

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.2.11)

2026-10-31 Started AayushBackendApplication in 3.456 seconds
2026-10-31 MongoDB connection established to: mongodb://localhost:27017/aayush_db
2026-10-31 Tomcat started on port(s): 8080 (http)
```

---

## Testing

### Run All Tests

```bash
cd backend

# Run tests (requires MongoDB running)
./mvnw test

# Or on Windows
mvnw.cmd test
```

### Run Specific Test

```bash
# Run only unit tests (no database required)
./mvnw test -Dtest=TriageServiceTest

# Run integration tests (requires MongoDB)
./mvnw test -Dtest=EmergencySessionControllerTest
```

---

## Verify MongoDB Connection

### Using MongoDB Shell (mongosh)

```bash
# Connect to MongoDB
mongosh mongodb://localhost:27017/aayush_db

# Show all databases
show dbs

# Use aayush_db
use aayush_db

# Show collections
show collections

# Query emergency sessions
db.emergency_sessions.find().pretty()

# Count documents
db.emergency_sessions.countDocuments()

# Exit
exit
```

### Using MongoDB Compass (GUI)

1. Open MongoDB Compass
2. Connect to: `mongodb://localhost:27017`
3. Select database: `aayush_db`
4. Browse collection: `emergency_sessions`

---

## API Testing

### Create Emergency Session

```bash
curl -X POST http://localhost:8080/api/emergency-sessions \
  -H "Content-Type: application/json" \
  -d '{
    "patientProfile": {
      "name": "Test Patient",
      "age": 75,
      "allergies": "Penicillin",
      "emergencyContact": "+91-9876543210"
    },
    "symptoms": "Chest pain and difficulty breathing",
    "language": "en"
  }'
```

### List All Sessions

```bash
curl -X GET http://localhost:8080/api/emergency-sessions
```

### Get Session by ID

```bash
curl -X GET http://localhost:8080/api/emergency-sessions/SOS-20261031-ABCD
```

### Update Session Status

```bash
curl -X PATCH http://localhost:8080/api/emergency-sessions/SOS-20261031-ABCD/status \
  -H "Content-Type: application/json" \
  -d '{
    "status": "ACKNOWLEDGED"
  }'
```

---

## Troubleshooting

### Issue: "Connection refused to MongoDB"

**Solution**:
```bash
# Check if MongoDB is running
# Windows:
sc query MongoDB

# Mac/Linux:
brew services list | grep mongodb
# or
sudo systemctl status mongod

# Docker:
docker ps | grep mongo
```

### Issue: "Tests fail with MongoDB connection error"

**Solution**:
1. Ensure MongoDB is running on port 27017
2. Check test configuration: `backend/src/test/resources/application-test.properties`
3. Verify no firewall blocking port 27017

### Issue: "Application starts but no data persists"

**Solution**:
```bash
# Check MongoDB connection string
echo $MONGODB_URI

# Verify MongoDB is accepting connections
mongosh mongodb://localhost:27017/aayush_db --eval "db.adminCommand('ping')"
```

### Issue: "Flapdoodle test errors on Windows"

**Solution**:
- Start Docker Desktop and use Docker Compose for MongoDB
- OR install MongoDB locally instead of using embedded MongoDB
- Tests will automatically connect to local MongoDB instance

---

## MongoDB Production Deployment

### MongoDB Atlas (Cloud) Setup

1. **Create Account**
   - Visit: https://www.mongodb.com/cloud/atlas
   - Sign up for free tier

2. **Create Cluster**
   - Choose: M0 Free Tier
   - Region: Closest to your users
   - Cluster Name: aayush-cluster

3. **Configure Network Access**
   - Add IP Address: `0.0.0.0/0` (for development)
   - For production: Whitelist specific IPs

4. **Create Database User**
   - Username: aayush_user
   - Password: Generate secure password
   - Role: Read and Write to any database

5. **Get Connection String**
   ```
   mongodb+srv://aayush_user:<password>@aayush-cluster.xxxxx.mongodb.net/aayush_db?retryWrites=true&w=majority
   ```

6. **Update Environment Variable**
   ```bash
   export MONGODB_URI="mongodb+srv://aayush_user:<password>@aayush-cluster.xxxxx.mongodb.net/aayush_db?retryWrites=true&w=majority"
   ```

---

## MongoDB Performance Tuning

### Create Indexes

```javascript
// Connect to MongoDB
use aayush_db

// Create indexes for better query performance
db.emergency_sessions.createIndex({ "sessionId": 1 }, { unique: true })
db.emergency_sessions.createIndex({ "status": 1 })
db.emergency_sessions.createIndex({ "createdAt": -1 })
db.emergency_sessions.createIndex({ "triageAssessment.urgencyLevel": 1 })

// Compound index for common queries
db.emergency_sessions.createIndex({ "status": 1, "createdAt": -1 })

// Verify indexes
db.emergency_sessions.getIndexes()
```

---

## Additional Resources

- **MongoDB Documentation**: https://docs.mongodb.com/
- **Spring Data MongoDB**: https://docs.spring.io/spring-data/mongodb/reference/
- **MongoDB Atlas**: https://www.mongodb.com/docs/atlas/
- **Docker Compose**: https://docs.docker.com/compose/

---

## Quick Reference

| Task | Command |
|------|---------|
| Start Docker MongoDB | `docker-compose up -d` |
| Stop Docker MongoDB | `docker-compose down` |
| View MongoDB logs | `docker-compose logs -f mongodb` |
| Connect to MongoDB shell | `mongosh mongodb://localhost:27017/aayush_db` |
| Run backend | `./mvnw spring-boot:run` |
| Run tests | `./mvnw test` |
| View collections | `mongosh` → `use aayush_db` → `show collections` |

---

**Need Help?** Check the full migration report: `docs/MONGODB_MIGRATION_REPORT.md`
