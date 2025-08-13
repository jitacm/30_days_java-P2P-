# 🔗 Java P2P File Sharer

A lightweight, decentralized file-sharing application that lets you share files directly between computers without any central server. Think of it as your own private file-sharing network where every computer is both a client and a server!

## 🤔 What is this Project?

This is a peer-to-peer (P2P) file sharing application built entirely in Java. Unlike traditional file sharing services that rely on central servers, this application creates a direct connection between your computer and others, allowing you to:

- Share files directly with friends or colleagues
- Search for files on connected peers
- Download files without going through any third-party servers
- Create your own mini file-sharing network

Perfect for sharing documents, images, or any files within a local network or between trusted computers over the internet.

## ✨ Key Features

**🌐 Truly Decentralized**: No central server needed - every peer is equal

**🚀 Concurrent Connections**: Handle multiple file transfers simultaneously using thread pools

**🔍 Smart File Discovery**: Search for files across all connected peers with keyword matching

**📁 Direct File Transfer**: Lightning-fast binary file transfers between peers

**💻 Simple Command Interface**: Easy-to-use command-line interface - no complex GUI needed

**🔒 Local Control**: You decide what files to share and who to connect with

## 🛠️ Technologies Used

- **Java 11+**: Core programming language
- **Socket Programming**: For direct peer-to-peer communication
- **Multithreading**: Concurrent handling of multiple connections
- **ExecutorService**: Thread pool management for better performance
- **File I/O Streams**: Efficient binary file transfer
- **Network Programming**: TCP/IP connections between peers

## 📋 Prerequisites

Before you can run this application, make sure you have:

**Java Development Kit (JDK) 11 or higher**

### Windows Installation
1. Download JDK from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
2. Run the installer and follow the setup wizard
3. Verify installation: Open Command Prompt and type `java -version`

### macOS Installation
```bash
# Using Homebrew (recommended)
brew install openjdk@11

# Or download from Oracle/OpenJDK websites
```

### Linux Installation
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-11-jdk

# CentOS/RHEL/Fedora
sudo yum install java-11-openjdk-devel

# Arch Linux
sudo pacman -S jdk11-openjdk
```

## 🚀 Getting Started

### Step 1: Get the Code
Clone this repository to your computer:
```bash
git clone https://github.com/Ashleesh/30_days_java-P2P-.git
cd 30_days_java-P2P-
```

### Step 2: Set Up Your Directories
The application needs specific folders to work properly. Don't worry, it will create them automatically, but here's what they're for:

**📂 shared/**: Put files here that you want to share with others
**📥 downloads/**: Files you download from other peers will appear here

Let's create a test file to share:
```bash
# Create the shared directory (if it doesn't exist)
mkdir -p shared

# Create a test file
echo "Hello from my P2P network! 🎉" > shared/hello.txt
```

### Step 3: Compile the Application
Navigate to your project directory and compile the Java code:

```bash
# Compile the Peer.java file
javac Peer.java
```

If you see no errors, you're ready to go! 🎉

## 🎮 How to Run

### Starting Your First Peer
Open your terminal/command prompt and start the first peer:

```bash
java Peer 9001
```

You should see:
```
Listening for peers on port 9001...
> 
```

Congratulations! Your first peer is now running and waiting for connections.

### Starting a Second Peer
Open another terminal window and start a second peer on a different port:

```bash
java Peer 9002
```

Now you have two peers running independently!

### Connecting Peers Together
In the second peer's terminal (port 9002), connect to the first peer:

```bash
> connect localhost 9001
Connected to localhost:9001
```

### Testing File Sharing

**🔍 Search for files:**
```bash
[localhost:9001]> search hello
[localhost:9001] Search results:
 - hello.txt
```

**📥 Download the file:**
```bash
[localhost:9001]> download hello.txt
File downloaded: hello.txt
```

Check your `downloads` folder - your file should be there! ✨

## 📖 Available Commands

**connect <host> <port>**: Connect to another peer
- Example: `connect 192.168.1.100 9001`

**search <keyword>**: Search for files containing the keyword
- Example: `search document` or `search .pdf`

**download <filename>**: Download a specific file
- Example: `download presentation.pdf`

**exit**: Close the application gracefully

## 🏗️ How It Works Under the Hood

### The Magic of P2P Architecture

Each peer in the network acts as both a **client** and a **server**:

**🖥️ Server Side**: Continuously listens for incoming connections on your specified port. When another peer connects and asks for files, it responds with available files or sends the requested file.

**💻 Client Side**: Provides the command-line interface where you can connect to other peers, search for files, and download them.

### The Connection Process

1. **Listening**: Each peer starts a server socket that listens on your chosen port
2. **Connecting**: When you use the `connect` command, your peer establishes a socket connection to another peer
3. **Communication**: Commands like `search` and `download` are sent as text over the socket connection
4. **File Transfer**: Files are transferred as binary data streams for maximum efficiency

### Thread Pool Magic 🧵

The application uses Java's ExecutorService with a thread pool of 10 threads. This means:
- Multiple peers can connect to you simultaneously
- File transfers don't block other operations
- The application remains responsive even under load

## 🌐 Network Configuration

### Running on Local Network
If you want to share files with computers on your local network:

1. Find your computer's IP address:
   - **Windows**: `ipconfig`
   - **macOS/Linux**: `ifconfig` or `ip addr show`

2. Start your peer: `java Peer 9001`

3. Other computers can connect using: `connect YOUR_IP_ADDRESS 9001`

### Firewall Considerations
Make sure your firewall allows connections on the port you're using. You might need to add an exception for Java or the specific port.

## 🔧 Troubleshooting

**"Port already in use" error**: Choose a different port number

**"Connection refused"**: Make sure the target peer is running and the port is correct

**"File not found"**: Ensure the file exists in the `shared` directory of the peer you're downloading from

**Slow file transfers**: This is normal for large files over slower networks

## 🤝 Contributing

Want to make this project even better? Here are some ideas:

- Add a graphical user interface (GUI)
- Implement file encryption for secure transfers
- Add support for resuming interrupted downloads
- Create a peer discovery mechanism
- Add file integrity checking with checksums

Feel free to fork this repository and submit pull requests!

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