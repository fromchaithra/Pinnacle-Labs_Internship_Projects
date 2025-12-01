Java Development Internship Projects – Pinnacle Labs (SEP25P21)

This repository contains the three major Java Swing applications I developed as part of the Java Development Internship at Pinnacle Labs.
Each project includes a clean user interface, robust functionality, persistent storage, and complete edge-case handling.

📌 Project Index
No.	Project Name	Description
1	📝 Notes Taking Application	Modern notes app supporting create, edit, delete, search, and auto-save with file persistence.
2	📚 Library Management System	Full CRUD system with borrowing/returning, search, CSV export, sorting, and local storage.
3	🛒 E-Commerce Cart System	Product catalog, cart management, GST checkout, and order persistence with a polished UI.
1️⃣ Notes Taking Application
✔ Description

A modern and user-friendly notes manager built using Java Swing.
Supports creating, editing, deleting, and searching notes with continuous auto-save.

⭐ Key Features

Add, edit, delete notes

Search notes with “note not found” message

Auto-save (notes_data.txt)

Modern UI (Segoe UI, pastel theme, styled buttons)

Prevents empty or duplicate titles

Loads safely even if file is missing/corrupted

▶️ Run
javac NotesTakingApp.java
java NotesTakingApp

2️⃣ Library Management System
✔ Description

A professional-grade Library Management System built in Java Swing, supporting book management, search, and borrower tracking.

⭐ Key Features

Add / Edit / Delete books

Borrow & Return system

Search by Title / Author / ISBN

Sortable table view

CSV Export (library_export.csv)

Persistent storage (library_data.ser)

Prevents empty fields & duplicate ISBNs

Handles invalid borrow/return operations

Enhanced, visually polished GUI

▶️ Run
javac LibraryManagementAppEnhanced.java
java LibraryManagementAppEnhanced

3️⃣ E-Commerce Cart System
✔ Description

A feature-rich E-Commerce Cart Application that simulates product browsing, cart updates, and checkout with GST.

⭐ Key Features

Product catalog

Add to cart / remove / update quantity

Checkout with GST, subtotal & grand total

Persistent cart (cart_data.ser)

Order history saved (orders.ser)

Clean and modern GUI with button styling

Handles invalid quantity, empty cart, duplicates, etc.

▶️ Run
javac EcommerceCartApp.java
java EcommerceCartApp

📁 Recommended Folder Structure
📂 Java-Development-Internship-PinnacleLabs
│
├── 📁 Project1_NotesApp
│   └── NotesTakingApp.java
│
├── 📁 Project2_LibraryManagement
│   └── LibraryManagementAppEnhanced.java
│
├── 📁 Project3_EcommerceCart
│   └── EcommerceCartApp.java
│
└── README.md

🧩 Technologies Used

Java (JDK 8+)

Java Swing (GUI Toolkit)

Object-Oriented Programming

File Handling & Serialization

CSV Export

MVC-structured logic

🧪 How to Run Any Project

Install Java JDK 8+

Open terminal

Navigate into the project folder

Compile → javac filename.java

Run → java MainClassName

Example:

javac NotesTakingApp.java
java NotesTakingApp

🎯 Skills Learned

GUI Development (Swing)

Real-world application modeling

Search, filtering, and validation

File-based persistent storage

User experience design

Debugging & edge-case handling

🙏 Acknowledgment

This work was completed as part of the
Java Development Internship – Pinnacle Labs (SEP25P21).
Thank you to the Pinnacle Labs team for the opportunity and guidance throughout the internship.# Pinnacle-Labs_Internship_Projects
