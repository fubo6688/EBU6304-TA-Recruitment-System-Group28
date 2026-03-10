# Java Upgrade Plan

**Project**: MyRecruitmentSystem  
**Session ID**: 20260311-001  
**Created**: 2026-03-11  
**Current Branch**: master  
**Current Commit**: 672021a

## Upgrade Goals

- **Java Version**: 11 → 21 (LTS)

## Guidelines

- Maintain backward compatibility where possible
- Ensure all tests pass after upgrade
- Minimal code changes required

## Options

- Run tests before and after the upgrade: true
- Working branch: appmod/java-upgrade-20260311-001

## Available Tools

### JDK Installations

| Version | Path | Notes |
|---------|------|-------|
| Java 11.0.16.1 | C:\Program Files\Microsoft\jdk-11.0.16.101-hotspot\bin | Current baseline |
| Java 21 | <TO_BE_INSTALLED> | Target version, will be installed in Step 1 |
| Java 24.0.1 | C:\Users\17192\.jdks\openjdk-24.0.1\bin | Available but not needed |

### Build Tools

| Tool | Version | Path | Usage |
|------|---------|------|-------|
| Maven | 3.3.9 | D:\Java\apache-maven-3.3.9\bin | Will be used for all build operations |

**Note**: Maven Wrapper not detected; using system Maven.

## Technology Stack

| Dependency | Current Version | Target Version | Compatibility | Notes |
|------------|----------------|----------------|---------------|-------|
| **Java Runtime** | 11 | 21 | ✅ Compatible | Direct upgrade supported |
| jakarta.servlet-api | 5.0.0 | 5.0.0 | ✅ Compatible | Already uses Jakarta EE 9+, compatible with Java 21 |
| maven-war-plugin | 3.3.2 | 3.3.2 | ✅ Compatible | Current version supports Java 21 |

## Derived Upgrades

| Component | Action | Reason |
|-----------|--------|--------|
| maven.compiler.source | 11 → 21 | Required for Java 21 upgrade |
| maven.compiler.target | 11 → 21 | Required for Java 21 upgrade |

**No additional dependency upgrades required** - The project uses Jakarta Servlet API 5.0.0 which is already compatible with Java 21.

## Key Challenges

1. **JDK Installation**: Java 21 is not currently installed and will need to be installed before the upgrade
2. **Maven Version**: Maven 3.3.9 is old (2015) but should work with Java 21; potential issues may require Maven upgrade
3. **Servlet Container Compatibility**: WAR deployment will require a servlet container (e.g., Tomcat 10+) that supports Jakarta EE 9+ and Java 21

## Upgrade Steps

### Step 1: Setup Environment
- **Objective**: Install Java 21 JDK
- **Actions**:
  - Download and install OpenJDK 21
  - Verify installation
- **Verification**: `java -version` shows Java 21
- **Expected Outcome**: Java 21 available for subsequent steps

### Step 2: Setup Baseline
- **Objective**: Establish baseline build/test metrics with current Java 11
- **Actions**:
  - Compile project with Java 11: `mvn clean test-compile` using Java 11
  - Run tests with Java 11: `mvn clean test` using Java 11
  - Document compilation and test results
- **Verification**: Build succeeds, record test pass/fail counts
- **Expected Outcome**: Baseline metrics documented for comparison

### Step 3: Update Java Version in POM
- **Objective**: Configure project to use Java 21
- **Actions**:
  - Update `maven.compiler.source` from 11 to 21
  - Update `maven.compiler.target` from 11 to 21
  - Update `pom.xml` properties
- **Verification**: Compile with Java 21: `mvn clean test-compile` using Java 21 JDK
- **Expected Outcome**: Project compiles successfully with Java 21

### Step 4: Final Validation
- **Objective**: Verify all upgrade goals met and achieve 100% test pass rate
- **Actions**:
  - Run full test suite: `mvn clean test` using Java 21
  - Fix any test failures iteratively
  - Verify all compilation and tests pass
  - Confirm Java 21 is the target version in pom.xml
- **Verification**: `mvn clean test` succeeds with 100% pass rate (or ≥ baseline)
- **Expected Outcome**: All upgrade success criteria met

## Plan Review

### Completeness Check
- ✅ All upgrade goals identified
- ✅ Technology stack analyzed
- ✅ Upgrade path designed with clear steps
- ✅ Verification criteria defined for each step

### Feasibility Assessment
- ✅ Java 21 is LTS and well-supported
- ✅ Dependencies are already Jakarta EE 9+ compatible
- ✅ No known breaking changes for this simple webapp
- ⚠️ Maven 3.3.9 is old but should work; may need upgrade if issues arise

### Known Limitations
- Maven Wrapper is not present; using system Maven which may have version inconsistencies across environments
- Deployment requires servlet container (Tomcat 10+, Jetty 11+) that supports Jakarta EE 9+ and Java 21

### Risk Assessment
- **Low Risk**: Simple project with minimal dependencies
- **Medium Risk**: Old Maven version may cause issues (mitigated by potential Maven upgrade step if needed)
