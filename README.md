# Quantum-Secure Email Communication System

A Java-based cybersecurity project that demonstrates secure email communication using cryptographic techniques and Quantum Key Distribution (QKD)-inspired concepts.

## Overview

This project simulates a secure communication environment where users can exchange encrypted emails while ensuring confidentiality, integrity, authenticity, and eavesdropping detection. The system combines modern cryptographic algorithms with quantum-inspired secure key exchange principles.

The application features a modern JavaFX graphical user interface for composing, encrypting, sending, receiving, and verifying secure emails.

---

## Features

* AES-256 based email encryption
* RSA digital signatures for authentication
* SHA-256 hashing for integrity verification
* QKD-inspired secure key exchange simulation
* Eavesdropping detection mechanism
* Secure email transmission workflow
* Interactive JavaFX GUI
* Message fingerprint generation
* Threat-aware communication handling
* Modular and scalable architecture

---

## Technologies Used

* Java
* JavaFX
* Maven
* AES-256 Encryption
* RSA Cryptography
* SHA-256 Hashing
* Object-Oriented Programming
* Cybersecurity Principles

---

## Project Structure

```text id="d4rjx0"
src/
 ├── main/
 │    ├── java/
 │    └── resources/
 ├── test/
pom.xml
README.md
```

---

## How It Works

1. Users generate secure cryptographic keys.
2. QKD-inspired key exchange establishes a secure session.
3. Emails are encrypted using AES-256.
4. RSA digital signatures verify sender authenticity.
5. SHA-256 hashing ensures message integrity.
6. The system detects potential eavesdropping attempts during transmission.
7. The receiver decrypts and verifies the email securely.

---

## Installation & Setup

### Clone Repository

```bash id="cb9xql"
git clone https://github.com/Saransh-29/QKD-Simulation-.git
```

### Open Project

Open the project in:

* IntelliJ IDEA
* VS Code
* Eclipse

### Run the Project

Using Maven:

```bash id="v9d7ow"
mvn clean install
mvn javafx:run
```

---

## Learning Objectives

This project was developed to explore:

* Secure communication systems
* Quantum-inspired cybersecurity concepts
* Cryptographic algorithms
* Secure software development
* Java GUI development using JavaFX

---

## Future Enhancements

* Real SMTP integration
* Multi-user networking support
* Database integration
* Real-time secure communication
* Quantum random number generation
* Advanced intrusion detection

---

## Author

Saransh Singh

---

## License

This project is developed for educational and academic purposes.
