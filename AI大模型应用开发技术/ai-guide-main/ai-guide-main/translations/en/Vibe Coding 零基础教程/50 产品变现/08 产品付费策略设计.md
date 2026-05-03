# Product Pricing Strategy Design

> Making Users Willing to Pay for Your Product

Hello everyone, I'm Yupi. Let me ask you a question first: If you've spent a lot of time developing a user-facing product, which payment method would you choose to charge users?

Would you opt for a one-time purchase, allowing users to buy permanent access to the product?

Or would you choose a subscription model, where users pay monthly or yearly for access to the product?

Both of these methods are mainstream monetization models I shared in my previous article. However, beyond these "bundled payment" product pricing mechanisms, there are more flexible "pay-as-you-go" mechanisms, such as point systems, package plans, and single-purchase options.

In this article, I'll use my own AI product as an example to explain a universal design for payment mechanisms—the **point system**.

Whether you're using Vibe Coding for personal projects or aiming to build a real product, understanding the point system can help you design more flexible pricing strategies.

## What is the Point System?

Unlike traditional models where users directly purchase products or subscribe for access (e.g., monthly memberships), the point system involves users purchasing **virtual points** that they can then use **as needed** to access services.

I'm sure you're no stranger to this payment mechanism. The diamonds, coins, or vouchers we top up in mobile games are classic examples of the point system.

![](https://pic.yupi.icu/1/image-20230719131457025.png)

And it's no exaggeration to say that the vast majority of domestic mobile games now adopt this mechanism.

Why is that?

## Why Choose the Point System?

Here, I'll explain the advantages of the point system from both product and development perspectives.

### Product Perspective

Let's think about the benefits of the point system from our own perspective. For example, ask yourself: Under what circumstances would I choose to top up points instead of buying a membership?

#### 1. Greater Flexibility

The foremost reason is undoubtedly flexibility. With points, I can selectively purchase multiple in-game items—for example, spending half on equipment and half on loot boxes—maximizing the value of the same amount of money for myself.

Our AI product allows users to directly purchase points and offers various point packages. Users can flexibly use these points for different services like AI chat, AI drawing, and more.

![](https://pic.yupi.icu/1/image-20230719144924694.png)

#### 2. Lower Trial Costs

Before fully understanding a product's features or becoming "addicted" to it, I wouldn't easily commit to a higher-priced membership. However, I might be willing to spend a small amount to try out premium features.

The point system provides users with lower trial costs. Instead of turning users away with high prices upfront, letting them experience the value of premium features first makes them more likely to continue paying, thereby increasing product revenue.

This approach has an additional benefit: It focuses on attracting users with product functionality rather than flashy marketing tactics (e.g., "lifetime memberships"), which is better for the product's reputation.

Our AI product also borrows tactics from many mobile games, allowing users to purchase points for as little as 6 yuan.

#### 3. Reduced Psychological Barriers

Before spending a higher fixed price on a product, I'd naturally consider many factors: How long will I use it? Which features will I use? Are these features worth the price?

The more users have to think about their decision, the less stable their purchase intent becomes.

With the point system, I don't need to overthink—for example, spending just 1 yuan for 10 points makes the purchase decision easier.

Additionally, the point system abstracts pricing into virtual points, allowing users to focus more on the product itself rather than the monetary cost. This makes spending points feel more natural.

#### 4. Increased User Retention

The point system enhances user retention and stickiness. Once I've purchased points, I subconsciously think, "I need to use up these points," which keeps me engaged with the product and strengthens my dependence on it.

In our AI product, to retain users and cultivate usage habits, we allow users to claim a certain number of free points daily. This practice is especially important in the early stages of the product.

![](https://pic.yupi.icu/1/image-20230719145440147.png)

#### 5. Adaptability to Diverse Business Needs

If a system has many features, users naturally don't want separate usage limits for each feature, such as:

- 10 AI chats remaining
- 5 AI drawings remaining
- 0 AI book-writing attempts remaining

If a user only wants to use the AI book-writing feature, they'd prefer to buy book-writing attempts directly rather than a bundled package. The product needs to support this option.

Conversely, if a user wants to use both AI drawing and AI book-writing, they'd prefer a bundled package for these two features. The product must also support this option.

As features multiply, the number of purchase options grows exponentially, and pricing them consistently becomes increasingly difficult.

By unifying all usage limits into "points," users can more easily understand their remaining usage without worrying about individual feature limits or calculating the most cost-effective purchase. They can freely allocate points as needed.

For the company, this not only simplifies pricing but also boosts overall sales by combining features or business lines. Tencent's Q币 (Q Coins) is a classic example—any Tencent product can potentially attract users to top up.

### Development Perspective

From a development standpoint, the biggest advantage of the point system is its **universality**.

Universal design brings three key benefits: unified concepts, simplified development, and enhanced scalability.

#### 1. Unified Concepts

You may have heard of the KISS principle (Keep It Simple, Stupid)—the simpler, the better.

In system development, fewer concepts lead to better design, development, and maintenance.

Instead of having each product feature correspond to separate usage limits, using the unified concept of **points** makes communication and understanding easier for both developers and product teams.

Consider this extreme example:

- AI chat consumes 1 "chat coin"
- AI drawing consumes 2 "drawing coins"
- AI book-writing consumes 3 "book-writing coins"
- User A has 2 "chat coins," 3 "drawing coins," and 9 "book-writing coins" left
- User A can still use AI chat 2 times, AI drawing 1 time, and AI book-writing 3 times

This requires tracking three types of features and their corresponding virtual currencies.

By unifying these into points, the same information becomes much clearer:

- AI chat, AI drawing, and AI book-writing consume 1, 2, and 3 points, respectively
- User A has 14 points remaining

#### 2. Simplified Development

Initially, when our system had fewer features, we planned to limit usage counts per feature. For example, users could only use AI chat 10 times daily and AI drawing 5 times.

This meant adding **feature-specific** validation and tracking logic in both the AI chat and AI drawing code, like this:

```java
// AI chat feature
void doChat() {
  // Get and validate remaining AI chat attempts
  chatLeftNum = user.getChatLeftNum()
  if (chatLeftNum < 1) {
    throw new Exception("Insufficient AI chat attempts")
  }
  // On success, deduct 1 AI chat attempt
  chatLeftNum--;
  chatLeftNum = chatLeftNum - 1;
}

// AI drawing feature
void doDraw() {
  // Get and validate remaining AI drawing attempts
  drawLeftNum = user.getDrawLeftNum()
  if (drawLeftNum < 1) {
    throw new Exception("Insufficient AI drawing attempts")
  }
  // On success, deduct 1 AI drawing attempt
  drawLeftNum = drawLeftNum - 1;
}
```

Additionally, the database would need to store more information, like:

| User id | AI Chat Attempts | AI Drawing Attempts | Others |
| ------- | ---------------- | ------------------- | ------ |
| 1       | 1                | 5                   | ...    |
| 2       | 2                | 6                   | ...    |

As the number of features grows, similar but not identical validation logic would proliferate, making the system harder to maintain. The database would also need more columns, consuming more storage.

With the point system, we can unify all usage variables (`chatLeftNum` and `drawLeftNum`) into a single `points` variable, with different features consuming different point amounts.

Example code:

```java
// AI chat feature
void doChat() {
  // Get and validate remaining points
  points = user.getPoints()
  if (points < 1) {
    throw new Exception("Insufficient AI chat attempts")
  }
  // On success, deduct 1 point
  points = points - 1;
}

// AI drawing feature
void doDraw() {
  // Get and validate remaining points
  points = user.getPoints()
  if (points < 2) {
    throw new Exception("Insufficient AI drawing attempts")
  }
  // On success, deduct 2 points
  points = points - 2;
}
```

We can further abstract point validation and deduction into reusable methods:

```java
// Validate remaining points
// Parameter 'type' represents the feature
void checkPoints(type) {
  needPoints = 1;
  if (type === "draw") {
    needPoints = 2;
  }
  points = user.getPoints();
  if (points < needPoints) {
    throw new Exception("Insufficient points")
  }
}

// Deduct points
// Parameter 'type' represents the feature
void usePoints(type) {
  needPoints = 1;
  if (type === "draw") {
    needPoints = 2;
  }
  points = user.getPoints();
  // Deduct points
  points = points - needPoints;
}
```

With these two methods, the original code simplifies to:

```java
// AI chat feature
void doChat() {
  checkPoints("chat");
  ...
  usePoints("chat")
}

// AI drawing feature
void doDraw() {
  checkPoints("draw");
  ...
  usePoints("draw");
}
```

Of course, this isn't the best practice yet, as the code still contains "hard logic"—determining point consumption based on `type`.

We can create a JSON configuration file to store point consumption for all features:

```json
[
  {
    "type": "chat",
    "usePoints": 1
  },
  {
    "type": "draw",
    "usePoints": 2
  },
]
```

Then write a helper method to fetch the point cost for a given feature:

```java
int getUsePoints(type) {
  list = readJSON("config.json")
  for (item : list) {
      if (item.type == type) {
          return item.usePoints;
      }
  }
  return 0;
}
```

Now, the point validation and deduction methods no longer need `if-else` logic!

```java
// Validate remaining points
void checkPoints(type) {
  needPoints = getUsePoints(type);
  points = user.getPoints();
  if (points < needPoints) {
    throw new Exception("Insufficient points")
  }
}

// Deduct points
void usePoints(type) {
  needPoints = getUsePoints(type);
  points = user.getPoints();
  // Deduct points
  points = points - needPoints;
}
```

To add a new feature, we only need to add an entry to the JSON file:

```json
{
  "type": "writeBook",
  "usePoints": 2
}
```

No matter how many new features are added, the database only needs to store the user's remaining points, eliminating the need for additional columns. This greatly simplifies development!

#### 3. Enhanced Scalability

Generally, the more universal the logic, the better the scalability.

Without the point system, adding separate usage limits for each feature would make the logic increasingly complex if we later needed to introduce additional consumption tiers within a feature.

For example, the AI drawing feature might support an additional "image-to-image" mode. If only basic AI drawing is used, it consumes 1 attempt; with image-to-image, it consumes 2 attempts. What if more modes are added? Would we need to consume 1.5 attempts? What if fractional attempts aren't supported?

Such logic could eventually corner the system, making it impossible to extend further.

With the point system, we can scale up the numbers—for example, making 1 attempt equal to 10 points. This allows us to set additional point costs for extra features, like 20 points for basic AI drawing plus 10 more for image-to-image, making the system more extensible.

## Applications of the Point System

Given these advantages, our AI product adopts the point system as its primary payment model.

Of course, the point system has drawbacks, such as being less intuitive or having non-fixed value. For products where "marketing > value," giving users trial costs might actually reduce sales compared to subscription models.

Thus, whether to use the point system depends on the specific context and product nature.

Our AI product combines the point system, membership, and package plans to offer users comprehensive choices. For example, members can claim more daily points and enjoy additional benefits, while non-members can purchase points at a discount. This approach retains the universality and flexibility of the point system.

Moreover, the point system is a **universal design** that can apply to many scenarios beyond payments, such as:

- E-commerce platforms where shopping points redeem products.
- Food delivery apps where order points unlock discounts.
- Education platforms where course-watching earns points for rewards.

## Final Thoughts

The point system is a flexible and universal pricing strategy design. It enhances user experience while simplifying development and improving scalability.

Key takeaways:

1. The point system offers flexibility, lowering trial costs and psychological barriers.
2. It adapts to diverse business needs.
3. It unifies concepts and simplifies development.
4. It's not a one-size-fits-all solution—use it contextually.
5. Combine it with memberships, packages, etc., for optimal results.

In the Vibe Coding era, AI can generate point system code for you. However, designing a pricing strategy that makes users willing to pay still requires careful thought.

Keep striving to design pricing strategies that satisfy users! 💪

## Recommended Resources

1) Yupi's AI Navigation Site: [AI Resource Directory, Latest AI News, Free AI Tutorials](https://ai.codefather.cn)  
2) Programming Navigation Learning Community: [Learning Paths, Coding Tutorials, Hands-on Projects, Job Hunting Guide, Q&A](https://www.codefather.cn)  
3) Programmer Interview Cheat Sheet: [Internship/Campus/Social Recruitment FAQs, Company Interview Questions](https://www.mianshiya.com)  
4) Resume Builder for Programmers: [Professional Templates, Rich Examples, Interview Ready](https://www.laoyujianli.com)  
5) 1-on-1 Mock Interviews: [Essential for Internship/Campus/Social Recruitment Offers](https://ai.mianshiya.com)