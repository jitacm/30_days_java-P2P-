# 📡 Java-P2P: A Peer-to-Peer File Sharer

A **console-based**, decentralized file-sharing application written in **Java**.
Each instance of the application acts as both a **client** and a **server**, enabling users to search and download files directly from other peers without a central server.

Think of it as your own private file-sharing network, perfect for learning **network programming**, **multithreading**, and **socket communication**.

---

## 📖 Table of Contents

1. [Overview](#-overview)
2. [Features](#-features)
3. [Key Terms](#-key-terms)
4. [Technologies Used](#-technologies-used)
5. [Prerequisites](#-prerequisites)
6. [Installation](#-installation)
7. [Usage Guide](#-usage-guide)
8. [Example Workflow](#-example-workflow)
9. [How It Works](#-how-it-works)
10. [Network Configuration](#-network-configuration)
11. [Troubleshooting](#-troubleshooting)
12. [Contributing](#-contributing)
13. [License](#-license)
14. [Educational Value](#-educational-value)

---

## 📜 Overview

This project demonstrates **network programming in Java** by building a fully functional **peer-to-peer (P2P)** file sharing system.
Unlike centralized services, there is **no single server** — every peer can connect to others, search for available files, and download them.

The application uses:

* **Multithreading** to handle multiple simultaneous connections.
* **TCP socket programming** for communication.
* **Binary streaming** for reliable file transfers.

---

## ✨ Features

* **Decentralized Architecture** — No central server; all peers are equal.
* **Dual Role Peers** — Each instance is both client and server.
* **Multithreaded Connection Handling** — Uses thread pools for efficiency.
* **File Search** — Find files on connected peers with keyword matching.
* **Direct File Transfer** — Fast binary transfers over TCP.
* **Simple CLI Interface** — Intuitive text commands for interaction.
* **Automatic Folder Setup** — Creates `shared/` and `downloads/` if missing.

---

## 📚 Key Terms

* **Peer**: A network participant acting as both client and server.
* **P2P**: Direct communication without a central server.
* **Socket**: Network endpoint for sending/receiving data.
* **TCP**: Reliable, ordered communication protocol.
* **Thread Pool**: Pre-instantiated threads for concurrent tasks.
* **Binary Stream**: Raw byte transfer for accurate file sharing.

---

## 🛠 Technologies Used

* **Java 11+**
* **Socket Programming** (TCP)
* **Multithreading** (ExecutorService)
* **File I/O Streams**
* **Command-line Interface Design**

---

## 💻 Prerequisites

* **Java Development Kit (JDK) 11 or higher**
* Basic CLI knowledge
* Same network connection or port forwarding for cross-network use

---

## ⚙️ Installation

### 1. Clone the Repository

```bash
git clone https://github.com/<your-username>/java-p2p-file-sharer.git
cd java-p2p-file-sharer
```

### 2. Create Required Folders

```bash
mkdir shared downloads
```

### 3. Add a Test File

```bash
echo "Hello, P2P World!" > shared/hello.txt
```

### 4. Compile the Application

```bash
javac Peer.java
```

---

## 🖥️ Usage Guide

Run a peer instance:

```bash
java Peer <port>
```

Example:

```bash
java Peer 9001
```

**Commands:**

| Command                 | Description              | Example                     |
| ----------------------- | ------------------------ | --------------------------- |
| `connect <host> <port>` | Connect to another peer. | `connect 192.168.1.10 9001` |
| `search <keyword>`      | Search for files.        | `search hello`              |
| `download <filename>`   | Download a file.         | `download hello.txt`        |
| `exit`                  | Exit the app.            | `exit`                      |

---

## 🛠 Example Workflow

**Terminal 1 (Peer 1):**

```bash
java Peer 9001
```

**Terminal 2 (Peer 2):**

```bash
java Peer 9002
connect localhost 9001
search hello
download hello.txt
```

Result: `hello.txt` is downloaded to Peer 2's `downloads/` folder.

---

## 🔧 How It Works

* **Server Thread**: Listens for connections and responds to search/download requests.
* **Client CLI**: Accepts user commands and sends requests to peers.
* **Multithreading**: Handles each connection in its own thread.
* **File Transfer**: Sends file size first, then binary data stream.

---

## 🌐 Network Configuration

* **Local Network**: Find IP using `ipconfig` (Windows) or `ifconfig` (macOS/Linux), then connect.
* **Firewall**: Allow chosen port for Java.

---

## 🔍 Troubleshooting

* **Port in use**: Choose another port.
* **Connection refused**: Ensure peer is online and port is correct.
* **File not found**: Verify in `shared/` folder.

---

## 🤝 Contributing

Potential improvements:

* GUI interface
* File encryption
* Peer discovery
* Resume downloads
* File integrity checks

---

## 📜 License

This project is licensed under the MIT License. Feel free to use, modify, and distribute it as you see fit!

## 🎯 Educational Value

This project is perfect for learning:
- Network programming in Java
- Socket communication
- Multithreading and concurrent programming
- File I/O operations
- Command-line interface design
- Peer-to-peer architecture concepts

---

**Happy file sharing! 🎉** 

If you found this project helpful, consider giving it a star ⭐ on GitHub!
