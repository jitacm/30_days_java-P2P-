# 📡 Java-P2P: A Peer-to-Peer File Sharer

A **console-based**, decentralized file-sharing application written in **Java**.  
Each instance of the application acts as both a **client** and a **server**, enabling users to search and download files directly from other peers without a central server.

---

## 📖 Table of Contents

1. [Overview](#📜-overview)
2. [Features](#✨-features)
3. [Key Terms](#📚-key-terms)
4. [Prerequisites](#💻-prerequisites)
5. [Installation](#⚙️-installation)
6. [Usage Guide](#🖥️-usage-guide)
7. [Example Workflow](#🛠-example-workflow)
7. [How It Works](#🔧-how-it-works)
9. [Project Structure](#📂-project-structure)
10. [Limitations](#⚠️-limitations)
11. [License](#📜-license)

---

## 📜 Overview

This project demonstrates **network programming in Java** by building a fully functional peer-to-peer (P2P) file sharing system.  
Unlike centralized file-sharing services, there is **no single server** — every peer can directly connect to others, search for available files, and download them.

The program uses:

- **Multithreading** to handle multiple simultaneous connections.
- **Socket programming** for TCP-based communication between peers.
- **Binary streaming** to send files reliably.

---

## ✨ Features

- **Decentralized Architecture** — No central server required; peers communicate directly.
- **Dual Role Peers** — Each instance acts as both client and server.
- **Multithreaded Connection Handling** — Thread pool for managing concurrent connections.
- **File Search** — Search shared directories on connected peers.
- **Direct File Transfer** — Download files over TCP using a binary data stream.
- **Simple CLI Interface** — Use text commands to control the peer and manage transfers.
- **Automatic Folder Setup** — Creates required `shared/` and `downloads/` folders if they don't exist.

---

## 📚 Key Terms

- **Peer**: A node in the network that can act as both client and server.
- **P2P (Peer-to-Peer)**: A network where each participant communicates directly without a central server.
- **Socket**: An endpoint for sending or receiving data across a network connection.
- **TCP (Transmission Control Protocol)**: A communication protocol that ensures reliable, ordered delivery of data.
- **Thread Pool**: A set of pre-instantiated threads ready to handle tasks, improving concurrency and performance.
- **Binary Stream**: A method of transmitting data as raw bytes, allowing accurate file transfer.

---

## 💻 Prerequisites

To compile and run the project, you will need:

- **Java Development Kit (JDK)** version 11 or higher.
- Basic understanding of command-line operations.
- (For cross-computer usage) Both systems must be on the same network or have port forwarding enabled.

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

Place a file inside `shared/`, for example:

```bash
echo "Hello, P2P World!" > shared/hello.txt
```

### 4. Compile the Application

```bash
javac Peer.java
```

---

## 🖥️ Usage Guide

### Start a Peer

Run the peer with:

```bash
java Peer <port>
```

Example:

```bash
java Peer 9001
```

### CLI Commands

| Command | Description | Example |
|---------|-------------|---------|
| `connect <host> <port>` | Connect to another peer. | `connect 192.168.1.10 9001` |
| `search <keyword>` | Search for files containing `<keyword>` in their name. | `search hello` |
| `download <filename>` | Download a file from the connected peer. | `download hello.txt` |
| `exit` | Exit the application. | `exit` |

---

## 🛠 Example Workflow

### Running Two Peers Locally

**Terminal 1:**

```bash
java Peer 9001
```

**Terminal 2:**

```bash
java Peer 9002
```

**In Peer 2 CLI:**

```bash
connect localhost 9001
search hello
download hello.txt
```

The file `hello.txt` from Peer 1's `shared/` folder will appear in Peer 2's `downloads/` folder.

---

## 🔧 How It Works

### Dual Components

- **Server Thread**: Listens for incoming connections and responds to search or download requests.
- **Client CLI**: Accepts user commands and communicates with other peers.

### Connecting to a Peer

The `connect` command opens a TCP socket to another peer's listening port.

### Searching for Files

1. The client sends `search <keyword>` to the server.
2. The server searches its `shared/` folder and returns matching filenames.

### Downloading a File

1. The client sends `download <filename>`.
2. The server responds with:
   - File size (long integer).
   - File content as binary stream.
3. The client reads exactly that many bytes and saves the file.

### Multithreading

Each incoming connection is handled in its own thread, allowing multiple downloads or searches simultaneously.

---

## 📂 Project Structure

```
.
├── Peer.java          # Main application code
├── shared/            # Folder containing files available to share
│   └── hello.txt
└── downloads/         # Folder where downloaded files will be saved
```

---

## 📜 License

This project is licensed under the MIT License — see the `LICENSE.md` file for details.
