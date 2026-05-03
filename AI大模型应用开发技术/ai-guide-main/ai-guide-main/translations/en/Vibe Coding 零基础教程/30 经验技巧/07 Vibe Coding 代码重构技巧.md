# Vibe Coding Code Refactoring Techniques

> Avoiding AI-generated spaghetti code chaos

Hello, I'm Yupi.

You may have encountered this situation: when you first started using AI for projects, the code was clear and concise, looking very pleasant. But as more features were added, the code gradually became messy. Eventually, you became afraid to touch this code because changing one part might affect other areas.

This is particularly common in Vibe Coding because AI may only focus on "whether it can run" while neglecting "whether it's maintainable." Below, I'll teach you how to identify and pay down technical debt, keeping your code elegant at all times.

## 1. What is Technical Debt?

Technical debt is a vivid metaphor.

Imagine you're building a house. To finish quickly, you use some temporary solutions: the foundation isn't solid, the walls aren't straight, and the wiring is haphazard. The house is built and habitable, but there are many hidden dangers. If not fixed promptly, problems will grow over time, and the cost of fixing them will increase.

Technical debt works the same way. To implement features quickly, you (or the AI) adopt some suboptimal solutions. These solutions work at the time but plant hidden risks for the future. As the project develops, these risks become real issues, slowing down your progress.

This year's research found that AI-generated code is particularly prone to technical debt. AI excels at quickly implementing features but isn't good at considering long-term architecture and maintainability. It gives you "highly functional but systematically lacking architectural judgment" code.

### Signs of Technical Debt

How can you tell if your project has technical debt? The most obvious sign is that modifying the code becomes increasingly difficult, and you start fearing changes because you don't know what might be affected. If you often think, "I don't dare to touch this part" or "Changing this might affect that," it means the technical debt is already severe.

### The Harm of Technical Debt

The harm of technical debt is cumulative. At first, it might just be slightly messy code that doesn't affect functionality. But over time, the problems worsen.

- Development slows down because more time is spent understanding and modifying the code.
- Bugs increase because overly complex code is prone to errors.
- New features become hard to add because the existing architecture doesn't support them.
- Team collaboration suffers because no one fully understands the code.

The worst-case scenario is reaching a tipping point where you might have to rewrite the entire project. This is the "bankruptcy" of technical debt.

## 2. Common Issues in AI-Generated Code

When using AI for Vibe Coding, if context management is poor, requirements are unclear, or the AI is asked to implement overly complex features at once, the generated code may have quality issues. Below are some typical scenarios—understanding them will help you better guide the AI.

### Excessive Nesting

To ensure the code runs, the AI sometimes generates deeply nested code.

What is nesting?

It's ifs inside ifs, loops inside loops, like Russian nesting dolls. For example:

```typescript
function processData(data: any) {
  if (data) {
    if (data.items) {
      if (data.items.length > 0) {
        data.items.forEach(item => {
          if (item.active) {
            if (item.price > 0) {
              // Actual logic
            }
          }
        });
      }
    }
  }
}
```

This code is hard to read and maintain. A better approach is early returns:

```typescript
function processData(data: any) {
  if (!data?.items?.length) return;
  
  const activeItems = data.items.filter(item => 
    item.active && item.price > 0
  );
  
  activeItems.forEach(item => {
    // Actual logic
  });
}
```

Clearly, the second version is more readable and easier to understand.

### Code Duplication

The AI might not actively reuse code but instead generate new code for each requirement.

For example, suppose you ask the AI to implement a user list page, an article list page, and a comment list page separately. The AI might give you three nearly identical sets of code, differing only in data fields. Or you might have code like this in multiple components:

```typescript
const handleSubmit = async () => {
  setLoading(true);
  try {
    const response = await fetch('/api/data', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });
    const result = await response.json();
    setData(result);
  } catch (error) {
    console.error(error);
  } finally {
    setLoading(false);
  }
};
```

This duplicated code should be extracted into a shared function or custom Hook.

### Lack of Abstraction

The AI tends to write concrete, direct code rather than abstract, reusable code.

For example, if you want to display a user list and an article list, the AI might give you two completely independent components, even though their structures are nearly identical.

A better approach is to create a generic list component and reuse it with different data and rendering functions.

### Arbitrary Naming

The AI sometimes uses relatively arbitrary names like `data`, `result`, `temp`, or `handleClick`. These names don't accurately convey intent, making the code harder to understand.

This might happen because your requirements aren't specific enough, and the AI doesn't know the true purpose of the variable or function.

Good names should be **self-explanatory**, like `userData`, `apiResponse`, `temporaryBuffer`, or `handleLoginButtonClick`.

If you notice the AI's naming is too arbitrary, you can explicitly state in your requirements: "Please use meaningful variable and function names that clearly express their purpose."

## 3. How to Use AI for Code Refactoring?

If AI can create technical debt, can it also help pay it down?

The answer is yes. This is one of the big advantages of Vibe Coding—**AI can both write code quickly and refactor it quickly**.

### Let the AI Identify Problems

You can paste your code to the AI and ask it to review it professionally, helping you identify issues.

````markdown
Please review this code and suggest improvements:
```typescript
[Paste your code here]
```

Analyze it from the following perspectives:
1. Is there any duplicated code?
2. Are the functions too long?
3. Are the names clear?
4. Is there excessive nesting?
5. Can shared logic be extracted?
````

The AI will give you a detailed analysis report.

### Let the AI Provide Refactoring Solutions

Once problems are identified, ask the AI for refactoring solutions, such as:

- You mentioned this code has duplicated logic. Please provide a refactoring solution to extract the duplicated parts into a shared function.
- This function is too long. Please split it into smaller functions, each doing one thing.

The AI will give you specific refactored code.

### Small-Step Refactoring

Note: It's not advisable to refactor the entire project at once—this is too risky, and the project might not even run afterward.

The correct approach is small-step refactoring, changing only a small part at a time.

For example, if you find a function is too long, don't split it into 10 smaller functions at once. First, split it into 2, test it, and then continue splitting. At each step, ensure functionality remains unchanged and tests pass.

This way, if something goes wrong, it's easy to revert.

### When to Refactor

When should you refactor?

My suggestions are:

1) Don't schedule dedicated time for refactoring. Instead, refactor as you go during daily development. When you spot an issue, fix it immediately—don't postpone it.

2) Refactor after completing a feature. Once the feature is done and tested, spend 10–15 minutes reviewing the code for potential improvements.

3) Refactor before adding new features. If existing code isn't suitable for new features, refactor it first to make it more extensible.

4) Schedule periodic refactoring. Dedicate half a day each month or major version to address accumulated technical debt.

All these steps can also be done with AI leading and humans validating. The key is ensuring functionality remains unchanged and tests pass at each step. Don't rush—take it step by step.

## 4. Modularity and Code Reuse

Modularity is key to avoiding technical debt. Moreover, modular code is also more AI-friendly—when you need to modify a feature, the AI only needs to read the relevant small module rather than a massive file with hundreds of lines. This helps the AI understand and modify the code more accurately.

### What is Modularity?

Modularity means dividing code into independent, reusable modules. Each module does one thing and does it well. Modules communicate through clear interfaces without interfering with each other.

Good modularity has these characteristics:

- High cohesion: Code within a module is closely related.
- Low coupling: Dependencies between modules are minimized.
- Single responsibility: Each module is responsible for only one thing.
- Clear interfaces: Module inputs and outputs are well-defined.

### Component Splitting

In front-end frameworks like React, components are the basic modules. You can think of components as independent parts of a page, like a navigation bar, search box, or user card.

But many people (including AI) write "clunky" components.

For example, for a user profile page, the AI might put all the logic in one component: data fetching, form validation, submission handling, error messages... The result is a giant component with hundreds of lines.

A better approach is to split it into smaller components:

```typescript
// Main component, responsible for coordination
function ProfilePage() {
  const { user, loading, error } = useUser();
  
  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage error={error} />;
  
  return (
    <div>
      <ProfileHeader user={user} />
      <ProfileForm user={user} />
    </div>
  );
}

// Subcomponents, each with a single responsibility
function ProfileHeader({ user }) {
  return (
    <div>
      <Avatar src={user.avatar} />
      <h1>{user.name}</h1>
    </div>
  );
}

function ProfileForm({ user }) {
  // Form logic
}
```

This way, each component is small and easy to understand and test.

Even if you're not familiar with front-end or React, you can understand the idea—break large features into small, independent parts, each doing one thing. This principle applies to all programming languages and frameworks.

### Function Extraction

When you notice the same code appearing in multiple places, extract it into a function.

For example, if you need to format dates in multiple places:

```typescript
// Duplicated code
const formattedDate1 = new Date(date1).toLocaleDateString('zh-CN');
const formattedDate2 = new Date(date2).toLocaleDateString('zh-CN');
const formattedDate3 = new Date(date3).toLocaleDateString('zh-CN');

// Extracted into a function
function formatDate(date: Date | string): string {
  return new Date(date).toLocaleDateString('zh-CN');
}

const formattedDate1 = formatDate(date1);
const formattedDate2 = formatDate(date2);
const formattedDate3 = formatDate(date3);
```

This not only reduces duplication but also makes the code easier to maintain. If you need to change the date format later, you only need to modify one place.

### Using Custom Hooks

In React, custom Hooks are a great way to reuse logic. Hooks are special functions for managing state and side effects.

💡 You don't need to understand terms like Hooks, components, or state management. Just tell the AI:

```markdown
This logic is duplicated in multiple places. Please help me extract it into a reusable module.
```

The AI will automatically handle abstraction and reuse for you.

For example, if multiple components need to fetch user data, you can extract this logic into a Hook and reuse it elsewhere:

```typescript
// Before extraction: Each component repeats this logic
function ProfilePage() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    fetchUser().then(setUser).catch(setError).finally(() => setLoading(false));
  }, []);
  
  // ...
}

// After extraction: Create a custom Hook
function useUser() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  useEffect(() => {
    fetchUser().then(setUser).catch(setError).finally(() => setLoading(false));
  }, []);
  
  return { user, loading, error };
}

// Usage is simple
function ProfilePage() {
  const { user, loading, error } = useUser();
  // ...
}
```

Custom Hooks make code cleaner and easier to test.

## 5. From Toy Projects to Commercial Products

Many people use AI to create toy projects—functional but not professional or profitable.

At this point, you might wonder: How can I turn this into a scalable commercial product?

### Characteristics of Toy Projects

Toy projects typically have these traits:

- All code is in one file, or file organization is messy.
- No clear architecture—code is written haphazardly.
- Lacks error handling—only considers the happy path.
- No tests—relies entirely on manual testing.
- Lots of hardcoding—configuration is mixed with code.

Such projects are good for demos or learning but aren't suitable for long-term maintenance and scaling as commercial products.

### Characteristics of Commercial Products

Commercial products should be:

- Clear directory structure—easy to see where things belong.
- Well-defined architecture, like MVC, MVVM, or other patterns.
- Robust error handling—accounts for various edge cases.
- Adequate testing—core features have test coverage.
- Configuration separation—environment variables, config files, and code are separate.
- Good documentation—includes READMEs, API docs, comments, etc.

Transitioning from a toy to a commercial product requires conscious effort to improve code quality and refactor.

![](https://pic.yupi.icu/1/projectpk%E5%A4%A7.jpeg)

### Steps for Refactoring

How do you refactor a toy project into a commercial product?

I recommend doing it step by step:

1) Organize the directory structure. Group code by functionality or type into different folders—e.g., components in `components`, utility functions in `lib`, and type definitions in `types`.

2) Extract duplicated code. Identify repeated logic and extract it into shared functions or components. This significantly reduces code volume.

3) Split large files. Break big files into smaller ones, each responsible for one thing. For example, a large `utils.ts` can be split into `format.ts`, `validate.ts`, `storage.ts`, etc.

4) Add type definitions. If using TypeScript, add complete types to all functions and components. This helps uncover many potential issues.

5) Improve naming. Replace unclear variable and function names with descriptive ones. This makes the code easier to understand.

6) Add tests and documentation. Write tests for core features, create a README for the project, and add comments for complex logic.

All these steps can also be done with AI leading and humans validating. The key is ensuring functionality remains unchanged and tests pass at each step. Don't overdo it—take it one step at a time.

## 6. Case Study: Refactoring a Messy Project

Let me use a real example to show how to refactor a messy project.

### Initial State

Suppose you used AI to create a to-do app, with all code in a single `App.tsx` file—about 500 lines.

```typescript
// App.tsx (500 lines)
function App() {
  const [todos, setTodos] = useState([]);
  const [input, setInput] = useState('');
  const [filter, setFilter] = useState('all');
  const [loading, setLoading] = useState(false);
  
  // 100 lines of data-fetching logic
  useEffect(() => { /* ... */ }, []);
  
  // 50 lines of add logic
  const handleAdd = () => { /* ... */ };
  
  // 50 lines of delete logic
  const handleDelete = () => { /* ... */ };
  
  // 50 lines of edit logic
  const handleEdit = () => { /* ... */ };
  
  // 50 lines of filter logic
  const filteredTodos = todos.filter(/* ... */);
  
  // 200 lines of JSX
  return (
    <div>
      {/* Lots and lots of code */}
    </div>
  );
}
```

While the code works, all functional logic is crammed together, making it hard to read and maintain.

### Refactoring Steps

#### Step 1: Extract Custom Hooks

First, extract all to-do data-related logic (fetching, adding, deleting, updating) into a standalone Hook. This way, the main component doesn't need to worry about how data is managed—it just calls these methods.

```typescript
// hooks/useTodos.ts
function useTodos() {
  const [todos, setTodos] = useState([]);
  const [loading, setLoading] = useState(false);
  
  const addTodo = async (text: string) => { /* ... */ };
  const deleteTodo = async (id: string) => { /* ... */ };
  const updateTodo = async (id: string, text: string) => { /* ... */ };
  
  useEffect(() => {
    // Fetch data
  }, []);
  
  return { todos, loading, addTodo, deleteTodo, updateTodo };
}
```

#### Step 2: Split Components

Next, split the UI into smaller components. Each component handles only its own logic—e.g., the input box handles input, the list handles display, and the filter handles filtering. This keeps each component simple and easy to understand and modify.

```typescript
// components/TodoList.tsx
function TodoList({ todos, onDelete, onEdit }) {
  return (
    <div>
      {todos.map(todo => (
        <TodoItem 
          key={todo.id} 
          todo={todo} 
          onDelete={onDelete}
          onEdit={onEdit}
        />
      ))}
    </div>
  );
}

// components/TodoItem.tsx
function TodoItem({ todo, onDelete, onEdit }) {
  // Logic for a single to-do item
}

// components/TodoInput.tsx
function TodoInput({ onAdd }) {
  // Input box logic
}

// components/TodoFilter.tsx
function TodoFilter({ filter, onChange }) {
  // Filter logic
}
```

#### Step 3: Reassemble the Main Component

Finally, combine the extracted Hook and split components. Now, the main component only coordinates these parts, telling them what to do without worrying about how.

```typescript
// App.tsx (50 lines)
function App() {
  const { todos, loading, addTodo, deleteTodo, updateTodo } = useTodos();
 