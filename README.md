Java-P2P: A Peer-to-Peer File Sharer
---------------
A console-based, decentralized file-sharing application built with Java. Each instance of the application acts as both a client and a server, allowing users to search for and download files directly from other peers on the network without a central server.

This project is a practical example of network programming in Java, demonstrating multithreading for concurrent connections and socket programming for direct peer-to-peer communication.

Features
Decentralized Architecture: No central server is required; peers connect directly to each other.

Concurrent Handling: Uses a thread pool to manage multiple incoming connections simultaneously.

File Discovery: Search for files on a connected peer's shared directory.

Direct File Transfer: Download files directly from a peer using a binary data stream.

Simple CLI: An interactive command-line interface for connecting to peers and managing file transfers.

Prerequisites
To compile and run this project, you will need to have a Java Development Kit (JDK) version 11 or higher installed on your system.

Getting Started
Follow these steps to get the P2P application running and test a file transfer between two peers.

1. Clone or Download the Project
First, get the project files onto your local machine. If you have Git, you can clone the repository.

2. Set Up the File Structure
The application requires a specific directory structure to function correctly.

Create a folder named shared. This is where you will place files you want to share with other peers.

Create a text file inside the shared folder (e.g., hello.txt) and add some content to it.

Create an empty folder named downloads. This is where files you download from other peers will be saved.

Your directory should look like this:

.
├── Peer.java
├── shared/
│   └── hello.txt
└── downloads/

3. Compile the Application
Open your terminal in the root directory of the project and compile the Peer.java file.

javac Peer.java

4. Run Two Peer Instances
To simulate a P2P network, you need to run at least two instances of the application. Open two separate terminal windows.

In Terminal 1 (Peer 1):
Run the first peer on port 9001.

java Peer 9001

In Terminal 2 (Peer 2):
Run the second peer on a different port, for example, 9002.

java Peer 9002

5. Test the File Sharing
Now you can use the command-line interface to connect the peers and transfer a file.

In Peer 2's terminal, connect to Peer 1:

> connect localhost 9001

Once connected, still in Peer 2's terminal, search for the file you created:

[localhost:9001]> search hello

You should see hello.txt listed in the search results.

Finally, download the file:

[localhost:9001]> download hello.txt

The file will be downloaded and saved to your downloads folder. You can check the folder to verify that the transfer was successful.

How It Works
Dual Nature: Each Peer instance starts two main components:

A server thread that continuously listens for incoming connections from other peers.

A client CLI that allows the user to issue commands.

Connection: When a user types connect <host> <port>, the client component establishes a socket connection to another peer's server.

Communication: Once connected, commands like search and download are sent as plain text over the socket.

The receiving peer's server thread reads the command and calls the appropriate handler (handleSearch or handleDownload).

File Transfer:

When a download command is received, the serving peer first sends the total file size (as a long).

It then reads the file in chunks and writes the binary data directly to the socket's output stream.

The downloading peer reads the file size, then reads from the input stream until the specified number of bytes has been received, writing the data to a new file in its downloads folder.

License
-----------
This project is licensed under the MIT License - see the LICENSE.md file for details.