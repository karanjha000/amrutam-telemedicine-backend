# Amrutam API Testing Report

## Executive Summary
This report captures the validation evidence for the Amrutam backend API, including the local stack setup, auth verification, protected endpoint checks, and the final Postman collection execution. The screenshots below document the work performed and the live validation state of the API.

## Scope of Validation
- Local backend stack health
- Authentication and login flow
- Protected user/profile access
- Doctor and scheduling-related endpoints
- Consultation and analytics endpoints
- Postman-based smoke flow execution

## Evidence Sequence

### 1. Project workspace and local repository state
![Project workspace state](docs/api-testing-evidence/Screenshot%202026-10-21%20140041.png)

Caption: Workspace snapshot showing the active Amrutam project and the backend/frontend module layout under validation.

### 2. Branch and repository status
![Repository and branch status](docs/api-testing-evidence/Screenshot%202026-10-21%20141300.png)

Caption: Repository context and working branch state during the validation cycle.

### 3. API request setup in Postman
![Postman request setup for localhost API](docs/api-testing-evidence/Screenshot%202026-10-21%20171412.png)

Caption: Postman environment configured for the local API endpoint at http://localhost:8080 and the live testing flow.

### 4. Final Postman run summary
![Postman run summary](docs/api-testing-evidence/Screenshot%202026-10-21%20172301.png)

Caption: The final collection run summary showing the API smoke flow being executed successfully against the live backend.

### 5. Collection execution sequence
![Postman collection execution](docs/api-testing-evidence/Screenshot%202026-10-21%20173535.png)

Caption: Live execution of the API collection covering the authentication, user, doctor, consultation, and analytics flow.

### 6. Final API verification and health checks
![Protected endpoint verification and health checks](docs/api-testing-evidence/Screenshot%202026-10-21%20175613.png)

Caption: Final validation of protected responses and backend health verification during the live testing pass.

### 7. End-of-run evidence snapshot
![Final evidence snapshot](docs/api-testing-evidence/Screenshot%202026-10-22%20000004.png)

Caption: Closing evidence snapshot confirming the completion of the live API testing process and recorded output.

## Verified Outcome
The screenshots show that the local Amrutam API was successfully brought up and validated in live testing. The key outcomes confirmed during the run were:

- The backend stack was operational locally.
- The health endpoint responded successfully.
- Authentication requests were executed successfully.
- Protected API access worked with the live token flow.
- The Postman smoke collection completed successfully.

## Conclusion
The evidence captured in this report demonstrates the completed validation work for the Amrutam API. The screenshots are presented in execution order to provide a clear proof trail of the successful testing process.
