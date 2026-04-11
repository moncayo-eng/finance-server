# MoMoney — Entity Relationship Diagrams

## Phase 1: Foundation (Users, Households, Members)

```mermaid
erDiagram
    users {
        uuid id PK
        varchar email UK "NOT NULL, unique"
        varchar name "NOT NULL"
        varchar google_id UK "nullable, unique"
        varchar password_hash "nullable (not needed for OAuth)"
        varchar avatar_url "nullable"
        boolean email_verified "DEFAULT false"
        timestamp created_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
    }

    households {
        uuid id PK
        varchar name "NOT NULL"
        int max_members "DEFAULT 6"
        timestamp created_at "DEFAULT now()"
        timestamp updated_at "DEFAULT now()"
    }

    household_members {
        uuid id PK
        uuid household_id FK "NOT NULL"
        uuid user_id FK "NOT NULL"
        varchar role "NOT NULL (OWNER, MEMBER)"
        varchar display_name "nullable (override within household)"
        timestamp joined_at "DEFAULT now()"
    }

    users ||--o{ household_members : "belongs to"
    households ||--o{ household_members : "has"
```

### Phase 1 Notes
- A user can belong to **multiple households** (e.g., one with partner, one with roommates)
- Each `household_members` row links a user to a household with a role
- `OWNER` can manage settings, invite/remove members; `MEMBER` can add expenses and view data
- `display_name` lets a user go by a different name per household (nullable, falls back to `users.name`)
- `google_id` and `password_hash` are both nullable — a user has one or both depending on auth method
- Unique constraint on `(household_id, user_id)` prevents duplicate membership

---

## Full Vision: All Entities

```mermaid
erDiagram
    users {
        uuid id PK
        varchar email UK
        varchar name
        varchar google_id UK
        varchar password_hash
        varchar avatar_url
        boolean email_verified
        timestamp created_at
        timestamp updated_at
    }

    households {
        uuid id PK
        varchar name
        int max_members
        timestamp created_at
        timestamp updated_at
    }

    household_members {
        uuid id PK
        uuid household_id FK
        uuid user_id FK
        varchar role
        varchar display_name
        timestamp joined_at
    }

    categories {
        uuid id PK
        uuid household_id FK "nullable (null = system default)"
        varchar name
        varchar icon
        varchar color
        varchar type "FIXED or VARIABLE"
        boolean is_system "DEFAULT false"
        boolean is_active "DEFAULT true"
        timestamp created_at
    }

    expenses {
        uuid id PK
        uuid household_id FK
        uuid category_id FK
        uuid paid_by_user_id FK
        varchar responsible_type "SHARED or INDIVIDUAL"
        uuid responsible_user_id FK "nullable (null if SHARED)"
        numeric_12_2 amount
        varchar description
        date date
        varchar notes
        varchar receipt_url
        boolean is_settled "DEFAULT false"
        uuid settlement_id FK "nullable"
        uuid recurring_expense_id FK "nullable"
        boolean ai_categorized "DEFAULT false"
        uuid import_source_id "nullable"
        timestamp created_at
        timestamp updated_at
    }

    incomes {
        uuid id PK
        uuid household_id FK
        uuid user_id FK
        numeric_12_2 amount
        varchar description
        varchar frequency "MONTHLY, BIWEEKLY, ANNUAL, ONE_TIME"
        date effective_date
        date end_date "nullable"
        timestamp created_at
        timestamp updated_at
    }

    budgets {
        uuid id PK
        uuid household_id FK
        uuid category_id FK
        numeric_12_2 monthly_limit
        varchar effective_from "YYYY-MM"
        varchar effective_to "nullable, YYYY-MM"
        timestamp created_at
        timestamp updated_at
    }

    settlements {
        uuid id PK
        uuid household_id FK
        uuid from_user_id FK
        uuid to_user_id FK
        numeric_12_2 amount
        date period_start
        date period_end
        timestamp settled_at
        varchar notes
        timestamp created_at
    }

    recurring_expenses {
        uuid id PK
        uuid household_id FK
        uuid category_id FK
        uuid paid_by_user_id FK
        varchar responsible_type
        uuid responsible_user_id FK "nullable"
        numeric_12_2 amount
        varchar description
        varchar frequency "WEEKLY, BIWEEKLY, MONTHLY, ANNUAL"
        int day_of_month "nullable"
        date start_date
        date end_date "nullable"
        boolean is_active "DEFAULT true"
        date last_generated_date "nullable"
        timestamp created_at
        timestamp updated_at
    }

    savings_goals {
        uuid id PK
        uuid household_id FK
        varchar name
        numeric_12_2 target_amount
        numeric_12_2 current_amount "DEFAULT 0"
        date target_date "nullable"
        varchar icon "nullable"
        varchar color "nullable"
        boolean is_active "DEFAULT true"
        timestamp created_at
        timestamp updated_at
    }

    savings_contributions {
        uuid id PK
        uuid savings_goal_id FK
        uuid user_id FK
        numeric_12_2 amount
        date date
        varchar notes "nullable"
        timestamp created_at
    }

    investment_accounts {
        uuid id PK
        uuid household_id FK
        uuid user_id FK "nullable (null = joint)"
        varchar name
        varchar institution
        varchar account_type "BROKERAGE, 401K, IRA, ROTH_IRA, CRYPTO, OTHER"
        varchar currency "DEFAULT USD"
        varchar notes "nullable"
        boolean is_active "DEFAULT true"
        timestamp created_at
        timestamp updated_at
    }

    investment_snapshots {
        uuid id PK
        uuid investment_account_id FK
        date date
        numeric_14_2 balance
        numeric_14_2 contributions "nullable"
        varchar notes "nullable"
        timestamp created_at
    }

    %% Relationships
    users ||--o{ household_members : "belongs to"
    households ||--o{ household_members : "has"
    households ||--o{ categories : "owns"
    households ||--o{ expenses : "contains"
    households ||--o{ incomes : "tracks"
    households ||--o{ budgets : "sets"
    households ||--o{ settlements : "settles"
    households ||--o{ recurring_expenses : "schedules"
    households ||--o{ savings_goals : "saves toward"
    households ||--o{ investment_accounts : "tracks"
    categories ||--o{ expenses : "classifies"
    categories ||--o{ budgets : "budgeted by"
    categories ||--o{ recurring_expenses : "categorizes"
    users ||--o{ expenses : "paid by"
    users ||--o{ incomes : "earns"
    users ||--o{ savings_contributions : "contributes"
    users ||--o{ investment_accounts : "owns"
    settlements ||--o{ expenses : "settles"
    recurring_expenses ||--o{ expenses : "generates"
    savings_goals ||--o{ savings_contributions : "receives"
    investment_accounts ||--o{ investment_snapshots : "snapshots"
```

### Relationship Summary

| Relationship | Type | Notes |
|---|---|---|
| User -> HouseholdMember | One-to-Many | A user can be in multiple households |
| Household -> HouseholdMember | One-to-Many | A household has up to `max_members` members |
| Household -> Category | One-to-Many | System categories have `household_id = NULL` |
| Household -> Expense | One-to-Many | All expenses scoped to a household (tenant isolation) |
| Household -> Income | One-to-Many | Income per user per household |
| Household -> Budget | One-to-Many | Budgets per category per month |
| Household -> Settlement | One-to-Many | Settlements between two users in same household |
| Household -> RecurringExpense | One-to-Many | Templates that generate expenses |
| Household -> SavingsGoal | One-to-Many | Shared or individual savings goals |
| Household -> InvestmentAccount | One-to-Many | Individual or joint accounts |
| Category -> Expense | One-to-Many | Every expense has a category |
| Category -> Budget | One-to-Many | Budgets target a specific category |
| User -> Expense (paid_by) | One-to-Many | Who actually paid |
| Settlement -> Expense | One-to-Many | Which expenses a settlement covers |
| RecurringExpense -> Expense | One-to-Many | Auto-generated expense records |
| SavingsGoal -> SavingsContribution | One-to-Many | Contributions toward a goal |
| InvestmentAccount -> InvestmentSnapshot | One-to-Many | Point-in-time balance records |

### Key Constraints
- **Tenant isolation**: Every query scoped by `household_id` at the repository layer
- **Money types**: `NUMERIC(12,2)` for expenses/income/budgets, `NUMERIC(14,2)` for investment balances
- **Soft deletes**: Categories use `is_active` flag (never hard delete, preserves history)
- **Unique constraints**: `(household_id, user_id)` on `household_members`, `email` and `google_id` on `users`
