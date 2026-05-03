# Vibe Coding Full Stack Application Development

Hello, I'm Yupi.

In this article, I'll guide you through three complete full-stack projects: a personal blog system, a simple Q&A community, and an online store (Mini version).

What is a full-stack application?

Simply put, it's a complete application that includes a front-end interface, a back-end service, and database storage. Users interact with the front-end, data is processed by the back-end, and ultimately stored in the database. These projects will teach you how to develop both front-end and back-end using the Vibe Coding approach, handling real-world scenarios like user authentication and database operations.

It's important to note that this tutorial focuses more on guiding you through the thought process and project development workflow, aiming to help you learn how to use Vibe Coding for project development. You'll need to practice on your own. If you need more comprehensive Vibe Coding tutorials with images and videos, you can check out Yupi's original project实战 section.

## 1. Full Stack Development Basics

Before starting the projects, let's first understand the basic concepts of full-stack development.

Full-stack development consists of three parts: front-end (the interface users see), back-end (handling business logic), and database (storing data).

The front-end is responsible for displaying data and receiving user input, the back-end handles requests and business logic, and the database is responsible for persistent storage. The three communicate through API interfaces.

![](https://pic.yupi.icu/1/fullstackarchitecture%E5%A4%A7.jpeg)

For example, when a user publishes an article on a blog website, the front-end collects the article's title, content, and other information, then sends an HTTP request to the back-end. The back-end receives the request, validates the data, and calls the database interface to save the article. After successful saving, the back-end returns a success response to the front-end, which displays a "Published successfully" message.

There are many choices for mainstream full-stack technology stacks. For Vibe Coding, I recommend using modern, AI-friendly technology stacks. The front-end can use React or Vue, the back-end can use Node.js (Express, Nest.js) or Python (FastAPI), and the database can use MySQL, PostgreSQL, or MongoDB.

However, the specific technology used isn't the most important; understanding the full-stack development mindset is. Once you grasp the mindset, you can quickly adapt to a different technology stack.

Developing full-stack applications with Vibe Coding has a huge advantage: you don't need to master all the details of front-end and back-end development. You just need to know what functionality to implement, and AI will generate the code for you.

For example, you just need to tell the AI: "Create a user registration interface that receives a username, email, and password, validates them, and saves them to the database."

The AI will generate the complete back-end code for you, including parameter validation, password encryption, database operations, etc. This significantly lowers the barrier to full-stack development, allowing you to focus on business logic rather than getting bogged down in technical details.

## 2. Project实战 - Personal Blog System

The blog system is one of the most classic full-stack projects, involving article publishing, category management, comment interactions, and more. Through this project, you'll learn how to handle CRUD (Create, Read, Update, Delete) operations and basic user authentication.

This blog system needs to support article publishing, editing, deletion, and viewing. Articles can be categorized and tagged. Users can register, log in, and publish their own articles. Visitors can browse articles but cannot publish them. Articles support Markdown format and can include cover images. The homepage displays a list of articles, supporting pagination and search.

![](https://pic.yupi.icu/1/demoweb10.png)

For the technology stack, the front-end uses React + TypeScript + Vite, with Tailwind CSS for styling. The back-end uses Node.js + Express, and the database is MySQL. User authentication is handled with JWT (JSON Web Token). The article editor can reuse a previously built Markdown editor.

### Development Steps

1) Design the Database

The first step in development is designing the database. Before writing any code, design the database structure. Create a `database.md` file to define the tables needed and the fields in each table.

For example, the user table needs: id (primary key), username (unique), email (unique), password (encrypted storage), avatar (URL), created_at (creation time).

The article table needs: id, title, content (Markdown format), cover (cover image URL), category, tags (JSON array), author_id (foreign key linking to users), views (view count), created_at, updated_at.

The category table needs: id, name (category name), description (category description).

2) Write the Requirements Document

After designing the database, write the requirements document. Create a `PRD.md` file:

```markdown
# Personal Blog System PRD

## Core Features

### User Features
- Registration: username, email, password
- Login: returns JWT token
- Personal Center: view and edit personal information

### Article Features
- Publish Article: title, content, category, tags, cover
- Edit Article: modify published articles
- Delete Article: can only delete own articles
- View Article: display article details, increment view count
- Article List: supports pagination, category filtering, search

### Front-end Pages
- Homepage: article list
- Article Detail Page
- Write Article Page (requires login)
- Personal Center Page (requires login)
- Login/Register Page
```

3) Develop the Back-end with AI

With the document ready, you can start developing the back-end with AI. Open Cursor and start a conversation with the AI.

First, initialize the back-end project:

```
Please create a Node.js + Express back-end project:
1. Initialize the project, install express, mysql2, jsonwebtoken, bcrypt, etc.
2. Create the basic project structure: src/routes (routes), src/controllers (controllers), src/models (data models), src/middleware (middleware), src/config (configuration)
3. Configure the database connection
```

This prompt specifies what project to create, which dependencies to install, and what the project structure should look like.

Second, create the database tables:

```
Based on the design in database.md, create SQL statements for the database tables. Then create an initialization script to automatically create these tables.
```

The AI will generate the SQL statements for creating the tables based on your database design.

Third, implement user registration and login:

```
Implement user registration and login functionality:

Registration Interface (POST /api/auth/register):
- Receive username, email, password
- Validate parameters (username 3-20 characters, valid email format, password at least 6 characters)
- Check if username and email already exist
- Encrypt password with bcrypt
- Save to database
- Return success message

Login Interface (POST /api/auth/login):
- Receive username, password
- Verify user exists
- Verify password is correct
- Generate JWT token (valid for 7 days)
- Return token and user information
```

This prompt details the functionality requirements for both interfaces. The AI will implement the complete registration and login logic, including parameter validation, password encryption, JWT generation, etc.

Fourth, implement article CRUD:

```
Implement article CRUD functionality:

Create Article (POST /api/posts):
- Requires login (verify JWT token)
- Receive title, content, category, tags, cover
- Get author_id from token
- Save to database
- Return article information

Get Article List (GET /api/posts):
- Supports pagination (page, pageSize)
- Supports category filtering (category)
- Supports search (keyword, searches title and content)
- Returns article list and total count

Get Article Detail (GET /api/posts/:id):
- Returns article details
- Increments view count

Update Article (PUT /api/posts/:id):
- Requires login
- Can only update own articles
- Updates specified fields

Delete Article (DELETE /api/posts/:id):
- Requires login
- Can only delete own articles
```

This prompt covers all article operations. The AI will implement the complete CRUD functionality, including permission verification and data queries.

4) Develop the Front-end with AI

After the back-end is developed, you can start developing the front-end. It's recommended to first test the back-end interfaces with Postman or a similar tool to ensure they work correctly before developing the front-end. This avoids simultaneous issues on both ends, making it unclear where the problem lies.

First, create the front-end project:

```
Create a React + TypeScript + Vite front-end project, install react-router-dom, axios, react-markdown, etc. Configure routes:
- / Homepage (article list)
- /post/:id Article Detail
- /write Write Article (requires login)
- /profile Personal Center (requires login)
- /login Login
- /register Register
```

Second, implement user authentication:

```
Implement user authentication functionality:
1. Create AuthContext to manage login state and user information
2. Store token in localStorage
3. Encapsulate axios to automatically add token to request headers
4. Create ProtectedRoute component to protect pages requiring login
5. Implement login and register pages
```

This prompt explains how to implement user authentication. AuthContext is used to manage global login state, and ProtectedRoute is used to protect pages that require login.

Third, implement the article list page:

```
Implement the homepage article list:
1. Fetch article list, display title, cover, category, author, time
2. Supports pagination, 10 articles per page
3. Supports category filtering (category tags at the top)
4. Supports search (search box)
5. Clicking an article navigates to the detail page
```

Fourth, implement the article detail page:

```
Implement the article detail page:
1. Fetch article details by ID
2. Use react-markdown to render article content
3. Display author information, publish time, view count
4. If it's the user's own article, display edit and delete buttons
```

Fifth, implement the write article page:

```
Implement the write article page:
1. Use the previously built Markdown editor
2. Input title, select category, add tags, upload cover
3. Edit on the left, real-time preview on the right
4. Save button calls the create article interface
5. If in edit mode, load existing article data
```

### Development Tips

When developing full-stack applications, there are several tips to improve efficiency. First, use environment variables to manage configurations. Sensitive information like database connection details and JWT secrets should not be hardcoded; use environment variables instead. Create a `.env` file to store configurations and read them via `process.env` in the code.

Second, handle errors properly. API calls may fail, database operations may encounter errors; capture these errors and return friendly messages. You can have the AI help you create a unified error-handling middleware.

Finally, during development, use nodemon to automatically restart the back-end service and Vite's hot-reload feature to automatically refresh the front-end page. This way, you don't need to manually restart after modifying the code, significantly improving development efficiency.

### Extension Ideas

After completing the basic version, you can continue to expand functionality. For example, add a comment system where users can comment on articles; add a like feature to like favorite articles; support article drafts to save unpublished articles; integrate image uploads to cloud storage; add article archiving by month; support RSS subscriptions for easy reader subscriptions; optimize SEO to make articles more easily indexed by search engines.

## 3. Project实战 - Q&A Community

After completing the blog system, let's tackle a more complex project — a Q&A community. The Q&A community is more complex than the blog system, involving questions, answers, likes, and accepted answers. Through this project, you'll learn how to handle more complex data relationships and user interactions.

This Q&A community needs to support users asking and answering questions. Users can post questions, and other users can answer them. Both questions and answers support likes. The question asker can accept the best answer. The homepage displays a list of questions, supporting sorting by popularity, newest, and unanswered. Each question has tags and can be filtered by tags. Users have a points system; asking questions, answering, and having answers accepted all earn points.

![](https://pic.yupi.icu/1/demoweb11.png)

The technology stack is similar to the blog system, with React + TypeScript + Vite for the front-end, Node.js + Express for the back-end, and MySQL for the database.

### Development Steps

1) Design the Database

The first step in development is designing the database. Create a `database.md` file:

```markdown
# Q&A Community Database Design

## User Table (users)
- id, username, email, password, avatar
- points: points
- created_at

## Question Table (questions)
- id, title, content (Markdown)
- tags: tags (JSON array)
- author_id: question asker ID
- views: view count
- answers_count: answer count
- votes: like count
- is_solved: whether solved
- created_at, updated_at

## Answer Table (answers)
- id, content (Markdown)
- question_id: question ID
- author_id: answerer ID
- votes: like count
- is_accepted: whether accepted
- created_at, updated_at

## Like Table (votes)
- id, user_id, target_type (question/answer)
- target_id: target ID
- created_at
```

2) Develop with AI

After designing the database, you can start developing with AI. The development process is similar to the blog system, but there are a few key points to note.

The first key point is the points system:

```
Implement the points system:
- Asking a question: -5 points
- Answering a question: +10 points
- Answer accepted: +30 points
- Question liked: +5 points
- Answer liked: +10 points

Automatically update user points in relevant operations.
```

This prompt explains the rules for points. The AI will handle point changes automatically, such as adding 10 points after a user answers a question.

The second key point is the like feature:

```
Implement the like feature:
- Users can like questions and answers
- Each user can only like a target once
- After liking, the target's votes count increases by 1
- After unliking, the votes count decreases by 1
- Return whether the user has liked the target
```

The like feature needs to prevent duplicate likes, so it needs to record users' like actions. When returning data, inform the front-end whether the user has liked the target so the front-end can correctly display the like button's state.

The third key point is accepting answers:

```
Implement the accept answer feature:
- Only the question asker can accept answers
- Each question can only accept one answer
- After accepting, the question status changes to solved
- The answerer receives points reward
```

This logic needs to be implemented on the back-end to ensure correct permission control.

The fourth key point is sorting the question list:

```
Implement multiple sorting options for the question list:
- Newest: sorted by creation time descending
- Popular: sorted by like count descending
- Unanswered: filter questions where is_solved = false
Supports filtering by tags.
```

### Development Tips

When developing the Q&A community, there are a few areas to pay attention to. First is data consistency; for example, when liking, both the like table and the target's votes count need to be updated. Both operations should either succeed or fail together. You can have the AI help you implement database transactions to ensure data consistency.

Second is performance optimization. The question list may have a lot of data, so pagination is necessary. You can have the AI help you implement paginated queries, returning only one page of data at a time to avoid loading too much data at once and causing page lag.

Additionally, judging the like state is important. When returning the question or answer list, inform the front-end whether the current user has liked the target so the front-end can correctly display the like button's state (red if liked, gray if not).

### Extension Ideas

After completing the basic version, you can continue to expand functionality. For example, add a comment feature to comment on answers; add a follow feature to follow interesting questions or users; implement a notification system to notify the question asker of new answers; optimize the search feature using full-text search to improve search quality; add a leaderboard to display points rankings; implement a badge system where users earn badges by completing achievements.

## 4. Project实战 - Online Store

After mastering user interactions and the points system, let's tackle a project involving transaction processes. The online store is one of the most complex full-stack projects, involving product management, shopping cart, order processing, and more. Through this project, you'll learn how to handle e-commerce business logic.

This Mini version of the store needs to implement basic e-commerce functionality. Product display (list and details), shopping cart (add, delete, modify quantity), order management (create order, view order), user address management. Payment functionality is not required; after creating an order, it can be displayed as "Pending Payment." This reduces development complexity, allowing focus on core business processes.

![](https://pic.yupi.icu/1/demoweb12.png)

The technology stack is similar to the previous projects, with React + TypeScript + Vite for the front-end, Node.js + Express for the back-end, and MySQL for the database.

### Development Steps

1) Design the Database

The first step in development is designing the database. Create a `database.md` file:

```markdown
# Online Store Database Design

## User Table (users)
- id, username, email, password, avatar
- created_at

## Product Table (products)
- id, name, description, price, stock (inventory)
- images: image URLs (JSON array)
- category: category
- created_at, updated_at

## Shopping Cart Table (cart_items)
- id, user_id, product_id, quantity
- created_at

## Address Table (addresses)
- id, user_id, name (recipient), phone, province, city, district, detail
- is_default: whether default address
- created_at

## Order Table (orders)
- id, order_no (order number), user_id, address_id
- total_price: total price
- status: status (Pending Payment, Paid, Shipped, Completed, Canceled)
- created_at, updated_at

## Order Item Table (order_items)
- id, order_id, product_id, quantity, price (price at order time)
```

2) Develop with AI

After designing the database, you can start developing with AI. The development process is similar to the previous projects, but there are a few key points to note.

The first key point is the shopping cart feature:

```
Implement the shopping cart feature:

Add to Cart (POST /api/cart):
- Receive product_id, quantity
- If the product is already in the cart, accumulate the quantity
- If it's a new product, create a new record
- Check