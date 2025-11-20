# Restaurant Ordering System (Swing)

Simple desktop restaurant ordering demo built with Java Swing.

## Overview
A small GUI application demonstrating a menu, order creation (with quantities), discounts, dine-in / take-out options, and an in-memory order history. Uses Java records and BigDecimal for monetary calculations.

## Features
- Browse menu by category (Main Course, Dessert, Drink)
- Add items with quantity to the current order
- View running order and subtotal
- Apply discounts (No Discount, 10%, 20%)
- Select Dine-In (with table) or Take-Out; generates order numbers (A### for dine-in, B### for take-out)
- View simple order history stored in memory for the app session

## Requirements
- Java 16 or newer (records are used). Java 17+ recommended.

## Build & Run
From a command line in the directory containing `RestaurantOrderingSystem.java`:

1. Compile:
   javac RestaurantOrderingSystem.java

2. Run:
   java RestaurantOrderingSystem

(If you use an IDE like IntelliJ IDEA or Eclipse, import the file and run the `RestaurantOrderingSystem` class.)

## Usage
- Click a category on the left to open a menu window.
- Select an item, choose quantity, and click "Add to Order".
- The order list updates on the right; the total updates automatically.
- "Finish Order" prompts for dine-in/take-out, optional table selection, and discount; it then shows a receipt and saves the order to the in-memory history.
- "View History" opens a window with past orders for the current session.

## Notes & Limitations
- Order history is stored in-memory only; closing the app clears history.
- Monetary math uses BigDecimal for accuracy.
- No persistence, networking, or multi-user support — intended as a local demo.
- UI is a simple Swing-based prototype and not optimized for accessibility/localization.

## Contributing
- Small fixes and improvements are welcome. Open an issue or submit a patch to improve menu data, add persistence, or refactor the UI.

## License
MIT
