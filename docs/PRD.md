# MoMoney — Product Requirements Document

**Version**: 1.1
**Date**: 2026-04-02
**Authors**: Agustin & Maria Emilia
**Status**: Draft

---

## 1. Overview

### 1.1 Problem Statement
Couples and housemates who share expenses face a common problem: tracking who paid what, splitting costs fairly, managing budgets together, and settling up. Most resort to spreadsheets or basic apps that are either too simplistic (Splitwise only handles splits) or too complex (full accounting software). The result is high-friction manual tracking, no automation, no visual insights, and error-prone settlement calculations.

### 1.2 Solution
MoMoney is a multi-tenant web application (with future iOS native app) for households to track shared finances. It prioritizes frictionless expense entry, AI-powered automation (categorization, receipt scanning), income-based expense splitting, settlement tracking, budget management, and savings/investment tracking — all in one place.

### 1.3 Target Users
- **Primary**: Couples who share expenses and split costs proportionally based on income
- **Secondary**: Housemates, families, or any small group (2-6 people) sharing expenses
- **Multi-tenant**: Each household is an isolated tenant. The platform supports many independent households simultaneously.

### 1.4 Success Metrics
- Expense entry takes < 10 seconds
- 90%+ of expenses are auto-categorized correctly after 1 month of usage per household
- Active household members check the app at least weekly
- Monthly settlement is completed in under 2 minutes
- User retention: 60%+ of households active after 3 months
- Budget adherence improves within 3 months of use (tracked via category overspend frequency)

---

## 2. User Personas

### The Admin (Household Creator)
- Sets up the household, invites members, configures categories and budgets
- Comfortable with setup and configuration
- Wants bulk-import from bank CSV exports
- Interested in data, trends, charts, and reports
- Wants AI features to reduce manual work
- Example: Agustin — software engineer, power user

### The Daily User (Household Member)
- Wants the simplest possible experience for adding expenses
- Primarily uses phone — needs mobile-responsive or native experience
- Cares about settlement balance ("do I owe my partner or do they owe me?")
- Wants clear budget progress ("am I over budget on dining out?")
- Should never need to configure anything technical
- Example: Jule — adds expenses, checks budget status, settles up

### The Saver/Investor
- Wants to track savings goals and investment accounts alongside spending
- Wants to see net worth and savings rate over time
- May or may not be the same person as the Admin

---

## 3. Tech Stack

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| Backend | Spring Boot + Kotlin | Null safety for financial data, data classes reduce boilerplate, coroutines for async AI calls, first-class Spring support |
| Database | PostgreSQL | ACID compliance for financial transactions, NUMERIC type for precise money, full-text search, mature ecosystem |
| Migrations | Flyway | Version-controlled, repeatable SQL migrations |
| Frontend | React + TypeScript + Vite | Type safety, fast builds, large ecosystem |
| UI Components | Tailwind CSS + shadcn/ui | Utility-first styling, beautiful accessible components, no vendor lock-in |
| State Management | TanStack Query (server) + Zustand (client) | Separation of server/client state, built-in caching and optimistic updates |
| Charts | Recharts | Composable React components, good for financial dashboards |
| Authentication | Google OAuth 2.0 + Email/Password + JWT | Multi-provider auth, stateless API |
| AI | Claude API (Anthropic) | Auto-categorization (Sonnet), receipt OCR (vision), structured output via tool use |
| File Storage | Cloudflare R2 | S3-compatible, generous free tier for receipt images |
| Backend Deploy | Railway | Simple container deployment, integrated Postgres, affordable |
| Frontend Deploy | Vercel | Free tier, CDN, instant deploys from git |

---

## 4. Feature Requirements

### 4.1 Authentication, Authorization & Household Setup

#### F-AUTH-01: User Registration & Login
- **Priority**: P0
- **Description**: Secure multi-provider authentication for all users.
- **Acceptance Criteria**:
  - **Google OAuth** (primary): "Sign in with Google" button, auto-register on first login
  - **Email/password** (fallback): register with email + password, email verification required
  - Backend issues JWT access token (15-min) + refresh token (30-day, httpOnly secure cookie)
  - Subsequent page loads auto-authenticate via `/auth/me`
  - Rate limiting on login attempts (5 per minute per IP)
  - Account lockout after 10 consecutive failed password attempts

#### F-AUTH-02: Authorization & Multi-Tenancy
- **Priority**: P0
- **Description**: Household-scoped data isolation and role-based access control.
- **Acceptance Criteria**:
  - **Tenant isolation**: Every data query is scoped to the user's household. A user can NEVER access another household's data. Enforced at the repository/query layer, not just the controller.
  - **Roles per household**:
    - **Owner**: Full access. Can manage household settings, invite/remove members, delete household, manage categories, set budgets.
    - **Member**: Can add/edit/delete their own expenses, view all household expenses, view budgets and settlements, add income. Cannot manage household settings or remove other members.
  - A user can belong to multiple households (e.g., one with a partner, one with roommates) and switch between them
  - The active household is stored in the JWT or session, switchable via UI
  - **API enforcement**: Every endpoint validates that the authenticated user belongs to the requested household and has the required role
  - Spring Security method-level authorization annotations (`@PreAuthorize`) for role checks

#### F-AUTH-03: Household Creation & Invite
- **Priority**: P0
- **Description**: Users create households and invite members.
- **Acceptance Criteria**:
  - After first login, user is prompted to create a household or join an existing one via invite
  - Creator becomes the Owner
  - Owner can invite members by email (sends invite link)
  - Invited user signs up/logs in and is added as Member
  - Configurable household size (default: up to 6 members)
  - Owner can remove members (with confirmation)
  - Owner can transfer ownership to another member
  - A user with no household sees an onboarding flow: "Create household" or "Enter invite code"

#### F-AUTH-04: User Profile & Account Management
- **Priority**: P1
- **Description**: Users manage their own account.
- **Acceptance Criteria**:
  - Edit display name, avatar
  - Link/unlink Google account
  - Change password (if using email/password)
  - Delete account (with confirmation, cascades appropriately)
  - View list of households they belong to

---

### 4.2 Expense Tracking

#### F-EXP-01: Quick Add Expense
- **Priority**: P0
- **Description**: Add an expense with minimal friction. This is the single most important interaction in the app.
- **Acceptance Criteria**:
  - Persistent quick-add form accessible from any page (floating action button or top-of-page form)
  - Required fields: amount, description
  - Optional fields (with smart defaults): date (default: today), category (default: AI-suggested), responsible party (default: Both)
  - Submit with a single tap/click after filling amount + description
  - Confirmation toast with "undo" option
  - Form clears and is ready for the next entry immediately
- **Design Notes**:
  - Amount field should auto-focus and accept numeric input immediately
  - Category dropdown should show recently used categories first
  - "Who" selector: buttons for each household member + "Shared" (default highlighted)

#### F-EXP-02: Expense List & Filters
- **Priority**: P0
- **Description**: View all expenses with powerful filtering and sorting.
- **Acceptance Criteria**:
  - Scrollable list showing: date, description, category (with icon/color), amount, who paid, responsible party, settled status
  - Filters: date range, category, responsible party (any member or Shared), settled/unsettled
  - Sort by: date (default: newest first), amount, category
  - Inline edit: click any field to edit in-place
  - Bulk select for batch operations (delete, re-categorize)
  - Pagination or infinite scroll for large datasets

#### F-EXP-03: Expense Detail & Edit
- **Priority**: P1
- **Description**: Full detail view for editing any expense field.
- **Acceptance Criteria**:
  - Modal or page showing all expense fields
  - Editable: amount, description, date, category, responsible party, notes
  - View attached receipt image (if any)
  - Delete with confirmation
  - Audit trail: created date, last modified date

#### F-EXP-04: Recurring Expenses
- **Priority**: P1
- **Description**: Automatically create expenses on a schedule for fixed costs.
- **Acceptance Criteria**:
  - Create recurring expense template: amount, description, category, frequency (weekly/biweekly/monthly/annual), day of month, responsible party
  - System automatically creates expense records when due (backend scheduled job, runs daily)
  - List of all recurring expenses with ability to edit/pause/delete
  - Notification when a recurring expense is auto-created (optional)
- **Examples**: Rent on the 1st, Netflix on the 15th, electricity on the 20th

---

### 4.3 Income & Split Ratio

#### F-INC-01: Income Tracking
- **Priority**: P0
- **Description**: Record income for each person to calculate proportional expense splits.
- **Acceptance Criteria**:
  - Each user can add income records: amount, description, frequency (monthly/biweekly/annual/one-time), effective date, end date (optional)
  - Support multiple income sources per person (e.g., salary + side income)
  - Income is normalized to monthly for split ratio calculation
  - Display current split ratio prominently (e.g., "Member A: 66.7% / Member B: 33.3%")

#### F-INC-02: Income-Based Split Ratio
- **Priority**: P0
- **Description**: Automatically calculate what percentage each person should pay for shared expenses.
- **Acceptance Criteria**:
  - Ratio = person's monthly income / total household monthly income
  - Updates automatically when income records change
  - Historical: uses the ratio that was in effect at the time of each expense (income records have effective dates)
  - Display on dashboard and settlement pages

---

### 4.4 Categories & Budgets

#### F-CAT-01: Expense Categories
- **Priority**: P0
- **Description**: Organize expenses into categories for budgeting and analysis.
- **Acceptance Criteria**:
  - System-seeded defaults: Rent, Electric, Internet, Cellular, Insurance, Groceries, Restaurants, DoorDash/Delivery, Transportation, Entertainment, Shopping, Health, Personal Care, Subscriptions, Travel, Gifts, Miscellaneous
  - Users can add custom categories with name, icon (emoji), color, and type (fixed/variable)
  - Categories can be deactivated (soft delete) but not hard deleted (preserves historical data)
  - Categories are per-household

#### F-BUD-01: Budget Setting
- **Priority**: P1
- **Description**: Set monthly spending targets per category.
- **Acceptance Criteria**:
  - Set a monthly dollar limit for any category
  - Budgets have effective date ranges (can change month to month)
  - Copy previous month's budgets as starting point for new month

#### F-BUD-02: Budget Tracking & Visualization
- **Priority**: P1
- **Description**: See real-time progress against budget targets.
- **Acceptance Criteria**:
  - Budget overview page: grid of category cards
  - Each card shows: category name/icon, spent amount, budget limit, progress bar
  - Color coding: green (< 70%), yellow (70-90%), red (> 90%)
  - Click card to see the expenses in that category for the current month
  - Dashboard widget showing top-level budget health

#### F-BUD-03: Spending Velocity Alerts
- **Priority**: P2
- **Description**: Proactive warnings when spending pace suggests a budget will be exceeded.
- **Acceptance Criteria**:
  - Alert when spending in a category exceeds the pro-rated budget for the current day of the month
  - Example: "You've spent 80% of your Restaurants budget ($400/$500) and it's only April 15th"
  - Displayed as banner on dashboard and budget page
  - Optional push notification (future iOS) or email

---

### 4.5 Settlements

#### F-SET-01: Settlement Balance
- **Priority**: P0
- **Description**: Real-time display of who owes whom and how much.
- **Acceptance Criteria**:
  - Prominent display: "[Member A] owes [Member B] $X" or "All settled up!"
  - Calculated from all unsettled shared expenses (responsibleParty = SHARED)
  - Uses income-based split ratio to determine each person's share
  - Accounts for who actually paid each expense
  - Updates in real-time as expenses are added

#### F-SET-02: Settle Up Flow
- **Priority**: P0
- **Description**: Mark a period's shared expenses as settled.
- **Acceptance Criteria**:
  - "Settle Up" button shows: net amount owed, date range of unsettled expenses, number of expenses
  - Review list of all unsettled shared expenses before confirming
  - On confirm: creates a Settlement record, marks all included expenses as settled
  - Settlement history page with past settlements (date, amount, expenses included)
  - Cannot un-settle (but can edit individual expenses if needed)

---

### 4.6 Import & AI Features

#### F-IMP-01: CSV Bulk Import
- **Priority**: P1
- **Description**: Import expenses from bank/credit card CSV exports.
- **Acceptance Criteria**:
  - Upload CSV file (drag-and-drop or file picker)
  - Auto-detect known bank formats (Chase, Amex, Bank of America, Capital One — expand based on user's banks)
  - For unknown formats: column mapping UI (map CSV columns to: date, description, amount)
  - Preview all parsed rows before import
  - Each row shows AI-suggested category (see F-AI-01)
  - User can edit any row before confirming
  - Duplicate detection: flag rows matching existing expenses (same date + amount + similar description)
  - Batch import on confirm
  - Import history: track which expenses came from which import batch

#### F-AI-01: AI Auto-Categorization
- **Priority**: P1
- **Description**: Automatically classify expenses into categories using Claude API.
- **Acceptance Criteria**:
  - When an expense is created without a category, AI suggests one
  - Uses the household's category list + few-shot examples from previously categorized expenses
  - High confidence (> 85%): auto-assign category, mark as `aiCategorized = true`
  - Low confidence: show suggestion with "confirm/change" prompt
  - Learning: cache description → category mappings so repeated merchants (e.g., "TRADER JOE'S #123") don't require API calls
  - Works during manual entry, CSV import, and receipt scanning
  - User can always override AI category

#### F-AI-02: Receipt Scanning
- **Priority**: P2
- **Description**: Photograph a receipt and extract expense data automatically.
- **Acceptance Criteria**:
  - Camera capture (on mobile) or image upload
  - Send image to Claude vision API for structured extraction
  - Extract: merchant name, date, line items (optional), total amount, tax
  - Display extracted data in editable form
  - Auto-categorize based on merchant name
  - Save receipt image to cloud storage, linked to expense record
  - Handle common receipt formats (grocery stores, restaurants, gas stations)

#### F-AI-03: Natural Language Quick Add
- **Priority**: P3 (stretch)
- **Description**: Type a natural sentence to create an expense.
- **Acceptance Criteria**:
  - Text input: "lunch at chipotle $12.50"
  - AI parses into: date = today, description = "Chipotle", amount = $12.50, category = Restaurants
  - Shows parsed result for confirmation before saving
  - Handles variations: "$45 groceries at trader joes yesterday", "split uber $22"

---

### 4.7 Reports & Visualizations

#### F-RPT-01: Dashboard
- **Priority**: P1
- **Description**: At-a-glance view of financial health for the current month.
- **Acceptance Criteria**:
  - Summary cards: total income, total expenses, net savings, settlement balance
  - Spending by category: donut/pie chart
  - Recent expenses: last 10 entries
  - Budget health: mini progress bars for top categories
  - Quick-add button always accessible

#### F-RPT-02: Monthly Summary Report
- **Priority**: P1
- **Description**: Detailed breakdown of a specific month.
- **Acceptance Criteria**:
  - Select any month to view
  - Total income, total expenses, savings rate
  - Breakdown by category with amounts and percentages
  - Breakdown by responsible party (per member / Shared)
  - Comparison to previous month (change indicators)

#### F-RPT-03: Spending Trends
- **Priority**: P2
- **Description**: Visualize spending patterns over time.
- **Acceptance Criteria**:
  - Line chart: monthly total spending over 6/12 months
  - Stacked bar chart: spending by category over time
  - Filter by category to see individual trends
  - Income vs. expenses over time

#### F-RPT-04: Year-over-Year Comparison
- **Priority**: P3
- **Description**: Compare spending between years.
- **Acceptance Criteria**:
  - Side-by-side comparison of two years (or year-to-date vs. previous year)
  - By category and by month
  - Highlight significant changes (> 20% increase/decrease)

#### F-RPT-05: Export
- **Priority**: P2
- **Description**: Export data for external use or record-keeping.
- **Acceptance Criteria**:
  - Export expenses to CSV with all fields, filterable by date range and category
  - Export monthly report as PDF (formatted summary with charts)
  - Tax-friendly export: filter by category for deduction-eligible expenses

---

### 4.8 Tags (Cross-Cutting Labels)

#### F-TAG-01: Expense Tags
- **Priority**: P3
- **Description**: Optional tags for cross-cutting expense grouping beyond categories.
- **Acceptance Criteria**:
  - Add one or more tags to any expense (e.g., "vacation", "wedding", "home improvement")
  - Create tags on the fly while tagging an expense
  - Filter expense list by tag
  - Reports can be filtered by tag to see total cost of a tagged group
- **Use Case**: Tag all expenses during a trip as "Costa Rica 2026" to see total trip cost across food, transport, lodging categories

---

### 4.9 Savings & Investments

#### F-SAV-01: Savings Goals
- **Priority**: P1
- **Description**: Track progress toward savings goals (emergency fund, vacation, down payment, etc.).
- **Acceptance Criteria**:
  - Create savings goals with: name, target amount, target date (optional), icon, color
  - Log contributions to each goal (who contributed, amount, date)
  - Visual progress bar showing current vs. target
  - Dashboard widget showing active savings goals
  - Support both individual and shared (household) savings goals
  - Historical view of contributions over time

#### F-SAV-02: Investment Account Tracking
- **Priority**: P1
- **Description**: Track investment and retirement account balances over time for a household net worth view.
- **Acceptance Criteria**:
  - Add investment accounts with: name, institution (e.g., "Fidelity", "Vanguard", "Coinbase"), account type (Brokerage, 401k, IRA, Roth IRA, Crypto, Other), owner (which household member)
  - Periodically log account snapshots: balance on a given date, contributions made
  - No automatic bank/brokerage API connections (manual entry or CSV for now — keeps it simple and avoids security complexity)
  - Track total contributions vs. current value to see investment returns
  - Chart: account balance over time (line chart per account)
  - Chart: total portfolio allocation by account type (pie/donut chart)
  - Support individual accounts (owned by one member) and joint accounts

#### F-SAV-03: Net Worth Dashboard
- **Priority**: P2
- **Description**: Aggregate view of household financial health.
- **Acceptance Criteria**:
  - Total net worth = sum of all investment account balances + savings goal balances
  - Net worth trend chart over time (monthly data points)
  - Breakdown: liquid savings vs. retirement vs. brokerage vs. crypto
  - Savings rate: (income - expenses) / income, displayed monthly
  - Comparison to previous month/year

#### F-SAV-04: Savings Rate Tracking
- **Priority**: P2
- **Description**: Automatically calculate and display how much of income is being saved.
- **Acceptance Criteria**:
  - Monthly savings rate = (total income - total expenses) / total income
  - Display on dashboard as a percentage with trend indicator
  - Historical chart of savings rate over time
  - Highlight months where savings rate was above/below target (if target is set)

---

## 5. Data Model

### 5.1 Entity Overview

```
User ──┐
       ├── HouseholdMember ──── Household
User ──┘                          │
                                  ├── Category
                                  ├── Budget (per Category per month)
                                  ├── Expense (has Category, paid by User)
                                  ├── Income (per User)
                                  ├── Settlement (between Users)
                                  ├── RecurringExpense (generates Expenses)
                                  ├── SavingsGoal
                                  └── InvestmentAccount
                                        └── InvestmentSnapshot (point-in-time values)
```

### 5.2 Key Entities

**User**: id, email, name, googleId?, passwordHash?, avatarUrl, emailVerified, createdAt

**Household**: id, name, maxMembers (default 6), createdAt

**HouseholdMember**: householdId, userId, role (OWNER/MEMBER), displayName (within household), joinedAt

**Income**: userId, householdId, amount (NUMERIC 12,2), description, frequency (MONTHLY/BIWEEKLY/ANNUAL/ONE_TIME), effectiveDate, endDate?

**Category**: householdId?, name, icon, color, type (FIXED/VARIABLE), isSystem, isActive

**Expense**: householdId, categoryId, paidByUserId, responsibleType (SHARED/INDIVIDUAL), responsibleUserId? (null if SHARED), amount (NUMERIC 12,2), description, date, notes?, receiptUrl?, isSettled, settlementId?, recurringExpenseId?, aiCategorized, importSourceId?, createdAt, updatedAt

**Budget**: householdId, categoryId, monthlyLimit (NUMERIC 12,2), effectiveFrom (YYYY-MM), effectiveTo?

**Settlement**: householdId, fromUserId, toUserId, amount (NUMERIC 12,2), periodStart, periodEnd, settledAt?, notes?

**RecurringExpense**: householdId, categoryId, paidByUserId, responsibleType, responsibleUserId?, amount, description, frequency (WEEKLY/BIWEEKLY/MONTHLY/ANNUAL), dayOfMonth?, startDate, endDate?, isActive, lastGeneratedDate?

**SavingsGoal**: householdId, name, targetAmount (NUMERIC 12,2), currentAmount (NUMERIC 12,2), targetDate?, icon?, color?, isActive, createdAt

**SavingsContribution**: savingsGoalId, userId, amount (NUMERIC 12,2), date, notes?, createdAt

**InvestmentAccount**: householdId, userId?, name, institution, accountType (BROKERAGE/RETIREMENT_401K/RETIREMENT_IRA/RETIREMENT_ROTH/CRYPTO/OTHER), currency (default USD), notes?, isActive, createdAt

**InvestmentSnapshot**: investmentAccountId, date, balance (NUMERIC 14,2), contributions (NUMERIC 14,2)?, notes?, createdAt

### 5.3 Key Constraints
- All monetary amounts: NUMERIC(12,2) for expenses/income, NUMERIC(14,2) for investment balances (can be larger)
- Never use floating point for money
- **Tenant isolation**: Every query MUST be scoped by householdId. Enforced at repository layer via Spring's `@Filter` or explicit WHERE clauses — not just controller-level checks
- Expenses always belong to a household
- Categories can be system-level (householdId = null) or household-specific
- Settlements reference two users within the same household
- Soft deletes for categories (isActive flag) to preserve historical data
- Users can belong to multiple households; active household tracked in session/JWT
- `responsibleType` replaces hardcoded names — SHARED means split by income ratio, INDIVIDUAL means `responsibleUserId` owns it entirely

---

## 6. API Design

Base URL: `/api/v1`

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register with email + password |
| POST | `/auth/login` | Login with email + password |
| POST | `/auth/google` | Exchange Google OAuth token for JWT |
| POST | `/auth/refresh` | Refresh access token |
| GET | `/auth/me` | Get current user + households list |
| POST | `/auth/verify-email` | Verify email address |
| POST | `/auth/forgot-password` | Request password reset |
| POST | `/auth/reset-password` | Reset password with token |

### Household
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/households` | Create household (caller becomes Owner) |
| GET | `/households` | List user's households |
| GET | `/households/{id}` | Get household details + members |
| PUT | `/households/{id}` | Update household settings (Owner only) |
| DELETE | `/households/{id}` | Delete household (Owner only, with confirmation) |
| POST | `/households/{id}/invite` | Invite member by email (Owner only) |
| POST | `/households/{id}/join` | Join via invite code |
| DELETE | `/households/{id}/members/{userId}` | Remove member (Owner only) |
| POST | `/households/{id}/transfer-ownership` | Transfer Owner role (Owner only) |
| GET | `/households/{id}/split-ratio` | Get current income-based split |
| POST | `/households/{id}/switch` | Set as active household (updates JWT/session) |

### Expenses
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/expenses` | Create expense |
| GET | `/expenses` | List with filters (date, category, responsible, settled) |
| GET | `/expenses/{id}` | Get single expense |
| PUT | `/expenses/{id}` | Update expense |
| DELETE | `/expenses/{id}` | Delete expense |
| POST | `/expenses/bulk` | Bulk create (from CSV import) |
| POST | `/expenses/scan-receipt` | Upload receipt, get parsed data |

### Categories
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/categories` | List all categories |
| POST | `/categories` | Create custom category |
| PUT | `/categories/{id}` | Update category |
| DELETE | `/categories/{id}` | Deactivate category |

### Income
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/incomes` | List income records |
| POST | `/incomes` | Add income |
| PUT | `/incomes/{id}` | Update income |
| DELETE | `/incomes/{id}` | Delete income |

### Budgets
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/budgets` | List budgets |
| GET | `/budgets/status` | Current month: actual vs budget per category |
| POST | `/budgets` | Create/update budget |
| DELETE | `/budgets/{id}` | Delete budget |

### Settlements
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/settlements/balance` | Current unsettled balance |
| GET | `/settlements` | Settlement history |
| POST | `/settlements` | Settle up (create settlement) |
| GET | `/settlements/{id}/expenses` | Expenses in a settlement |

### Recurring Expenses
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/recurring-expenses` | List all |
| POST | `/recurring-expenses` | Create |
| PUT | `/recurring-expenses/{id}` | Update |
| DELETE | `/recurring-expenses/{id}` | Deactivate |

### CSV Import
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/csv/upload` | Upload CSV file |
| POST | `/csv/preview` | Parse + AI categorize, return preview |
| POST | `/csv/confirm` | Confirm and import previewed data |

### Reports
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/reports/monthly-summary` | Monthly breakdown |
| GET | `/reports/category-breakdown` | Spending by category for date range |
| GET | `/reports/trends` | Month-over-month trends |
| GET | `/reports/year-over-year` | YoY comparison |

### Savings Goals
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/savings-goals` | List savings goals |
| POST | `/savings-goals` | Create savings goal |
| PUT | `/savings-goals/{id}` | Update savings goal |
| DELETE | `/savings-goals/{id}` | Delete savings goal |
| POST | `/savings-goals/{id}/contributions` | Log a contribution |
| GET | `/savings-goals/{id}/contributions` | List contributions |

### Investment Accounts
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/investment-accounts` | List investment accounts |
| POST | `/investment-accounts` | Create investment account |
| PUT | `/investment-accounts/{id}` | Update account details |
| DELETE | `/investment-accounts/{id}` | Deactivate account |
| POST | `/investment-accounts/{id}/snapshots` | Log balance snapshot |
| GET | `/investment-accounts/{id}/snapshots` | List snapshots (for charting) |
| GET | `/investment-accounts/net-worth` | Aggregate net worth + breakdown |

### Export
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/export/csv` | Export expenses as CSV |
| GET | `/export/pdf` | Export monthly report as PDF |

---

## 7. UI/UX Design

### 7.1 Pages

| Page | Route | Key Elements |
|------|-------|-------------|
| Login | `/login` | Google sign-in + email/password, registration link |
| Register | `/register` | Email/password registration form |
| Onboarding | `/onboarding` | Create household or enter invite code |
| Dashboard | `/` | Summary cards, donut chart, recent expenses, budget health, savings goals, quick-add FAB |
| Expenses | `/expenses` | Quick-add form, filterable list, bulk operations |
| Import | `/import` | 4-step wizard: upload → map columns → preview → confirm |
| Scan Receipt | `/scan` | Camera/upload → review extracted data → save |
| Budgets | `/budgets` | Category cards grid with progress bars |
| Settlements | `/settlements` | Balance display, unsettled expenses, settle-up button, history |
| Savings | `/savings` | Savings goals with progress bars, contribution log |
| Investments | `/investments` | Account list, balance snapshots, net worth chart, portfolio breakdown |
| Reports | `/reports` | Charts, monthly summary, trends, savings rate, export buttons |
| Income | `/income` | Income list per person, split ratio display |
| Settings | `/settings` | Categories, recurring expenses, household management (invite/remove members), profile |

### 7.2 Design Principles
1. **Mobile-first**: Primary use is on phones. Every interaction must work well on small screens.
2. **Speed over completeness**: Quick-add should require only amount + description. Everything else has smart defaults.
3. **Glanceable**: The dashboard should answer "how are we doing this month?" in 3 seconds.
4. **Shared context**: Both users see the same data. The UI should always make it clear who did what.
5. **Progressive disclosure**: Simple by default, detailed on demand. Don't overwhelm with fields.

### 7.3 Key Interaction: Quick Add Expense
This is the most critical UX flow. Design options:

**Option A: Floating Action Button (FAB)**
- Persistent "+" button in bottom-right corner
- Tapping opens a bottom sheet with the form
- Amount (auto-focus, numeric keyboard on mobile), description, category picker, who (3 buttons)
- "Add" button saves and dismisses

**Option B: Always-Visible Top Bar**
- Inline form at top of the Expenses page
- Collapsed to a single "Add expense..." input on other pages, expands on focus
- Same fields as Option A

**Recommendation**: FAB for mobile, inline form for desktop. Both should be accessible from any page.

---

## 8. Non-Functional Requirements

### 8.1 Performance
- Page load: < 2 seconds
- Expense creation: < 500ms response time
- AI categorization: < 3 seconds (async, can show loading state)
- Receipt scanning: < 10 seconds

### 8.2 Security & Multi-Tenancy
- All API calls authenticated via JWT
- **Tenant isolation**: All data queries scoped by householdId at the repository layer. No endpoint can leak cross-household data.
- **Role-based access**: Owner vs Member permissions enforced via Spring Security `@PreAuthorize`
- **Row-level security**: Users can only edit/delete their own expenses (unless Owner)
- Password hashing: bcrypt with cost factor 12
- Rate limiting: login endpoints, AI endpoints, CSV upload
- Receipt images stored in private bucket, accessed via signed URLs (expire after 1 hour)
- No financial account credentials stored (CSV upload only, no bank API integration)
- HTTPS everywhere
- Input validation and sanitization on all endpoints (prevent XSS, SQL injection)
- CORS restricted to known frontend origins

### 8.3 Reliability
- PostgreSQL with daily backups
- Graceful degradation if Claude API is unavailable (manual categorization fallback)
- Optimistic UI updates with rollback on error

### 8.4 Cost (scales with users)
- **Early stage (< 100 households)**:
  - Railway backend: ~$5-10/mo
  - Vercel frontend: $0 (free tier)
  - PostgreSQL (Neon or Railway): $0-19/mo
  - Cloudflare R2: $0 (free tier, 10GB)
  - Claude API: ~$10-30/mo depending on usage
  - Email (Resend): $0 (free tier, 3k emails/mo)
  - **Total: ~$15-60/month**
- **Growth considerations**: Claude API costs scale per-user. Consider per-household usage limits or a freemium model if scaling beyond early adopters.

---

## 9. Migration Plan (from Spreadsheets)

Users migrating from Google Sheets or Excel can onboard with their historical data:

### Phase 1: CSV Export
1. Export existing spreadsheet data as CSV
2. Use the CSV import feature (F-IMP-01) to load historical data
3. Map spreadsheet columns to app fields via column mapping UI
4. AI categorizes historical expenses
5. Review and confirm import

### Phase 2: Parallel Running (Optional)
- Run both systems for 1-2 weeks to validate data accuracy
- Compare settlement calculations between spreadsheet and app
- Once confident, retire the spreadsheet

---

## 10. Implementation Phases

### Phase 1: Foundation (Weeks 1-2)
- Backend: Spring Boot + Kotlin project, Flyway migrations for all tables
- Auth: Google OAuth + email/password registration, JWT access/refresh tokens, email verification
- Multi-tenancy: Household entity, HouseholdMember with roles (OWNER/MEMBER), tenant-scoped repositories
- Authorization: Spring Security config, role-based `@PreAuthorize`, household-scoped data access
- Category CRUD with system defaults
- Frontend: React + Vite + Tailwind + shadcn/ui
- Pages: Login, Register, Onboarding (create/join household), App layout with sidebar/nav

### Phase 2: Core Expense Tracking (Weeks 3-4)
- Expense CRUD (backend + frontend) with tenant isolation
- Quick-add expense component (dynamic member list for "who" selector)
- Expense list with filters
- Income CRUD + split ratio calculation (supports N members)

### Phase 3: Settlements (Week 5)
- Settlement calculation engine (pairwise balances for 2+ members)
- Balance display + settle-up flow
- Settlement history

### Phase 4: Budgets + Dashboard (Week 6)
- Budget CRUD + status API
- Budget overview with progress bars
- Dashboard with summary cards + charts

### Phase 5: CSV Import (Week 7)
- CSV upload, parsing, bank format detection
- Column mapping UI
- Preview + duplicate detection + confirm

### Phase 6: AI Features (Week 8)
- Claude API integration
- Auto-categorization with caching
- Receipt scanning

### Phase 7: Savings & Investments (Week 9)
- Savings goals CRUD + contribution tracking
- Investment accounts + balance snapshots
- Net worth dashboard + portfolio breakdown chart
- Savings rate calculation (automatic from income - expenses)

### Phase 8: Reports + Charts (Week 10)
- Monthly summary, category breakdown, trends
- Income vs. expenses, savings rate over time, YoY
- Net worth trends
- CSV/PDF export

### Phase 9: Polish (Week 11)
- Recurring expenses + scheduler
- Spending alerts
- Household management UI (invite/remove members, transfer ownership)
- Mobile-responsive polish
- Error handling, loading/empty states

### Phase 10: iOS Native App (Future)
- SwiftUI app against same backend API
- Focus: quick-add + camera receipt scanning + push notifications

---

## 11. Open Questions

1. Which banks/credit cards should we prioritize for CSV format auto-detection?
2. Email notifications for settlements/budget alerts — from day 1, or in-app only to start?
3. Should the app support multiple currencies (for travel) from day 1, or add later?
4. Any specific chart types or visualizations that would be most valuable?
5. Should there be an approval flow for large expenses, or is trust-based fine?
6. Freemium model? What features (if any) would be gated for a potential paid tier?
7. Should investment tracking support manual entry only, or should we plan for brokerage API integration (e.g., Plaid) in the future?
8. Max household size — should we cap at 6, or make it configurable?

---

## Appendix A: Default Categories

### Fixed Expenses
| Category | Icon | Color |
|----------|------|-------|
| Rent | 🏠 | #4F46E5 |
| Electric | ⚡ | #EAB308 |
| Internet | 🌐 | #06B6D4 |
| Cellular | 📱 | #8B5CF6 |
| Insurance | 🛡️ | #059669 |

### Variable Expenses
| Category | Icon | Color |
|----------|------|-------|
| Groceries | 🛒 | #22C55E |
| Restaurants | 🍽️ | #F97316 |
| DoorDash/Delivery | 🛵 | #EF4444 |
| Transportation | 🚗 | #3B82F6 |
| Entertainment | 🎬 | #EC4899 |
| Shopping | 🛍️ | #A855F7 |
| Health | ❤️ | #F43F5E |
| Personal Care | 💇 | #14B8A6 |
| Subscriptions | 📺 | #6366F1 |
| Travel | ✈️ | #0EA5E9 |
| Gifts | 🎁 | #D946EF |
| Miscellaneous | 📦 | #78716C |
