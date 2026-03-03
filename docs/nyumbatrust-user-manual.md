# NyumbaTrust User Manual

## Cover Page

**Application Name:** NyumbaTrust  
**Version:** 1.0  
**Date:** February 28, 2026  
**Company:** [Your Company Name]  
**Description:** NyumbaTrust is a secure platform for property sales, escrow-managed transactions, and construction project payments. It helps buyers, sellers, contractors, inspectors, and administrators manage property and project workflows in one trusted system.

## Table of Contents

1. Introduction  
2. User Roles & Permissions  
3. Getting Started  
4. Marketplace Module  
5. Escrow Module  
6. Construction Module  
7. Notifications & Status Indicators  
8. Admin Panel  
9. Security & Data Protection  
10. Frequently Asked Questions (FAQ)  
11. Troubleshooting Guide  
12. Support & Contact

## 1. Introduction

### What Is NyumbaTrust

NyumbaTrust is a digital platform designed to support secure property transactions and construction project payments. It combines a property marketplace, escrow services, and milestone-based project management into one system.

It is built for:

- Property buyers
- Property sellers
- Project owners
- Contractors
- Inspectors
- Administrators
- Support and operations teams

### Why Escrow Is Important

Escrow protects both the buyer and the seller during a transaction.

Instead of sending money directly to another party:

1. The buyer places funds into a secure holding account.
2. The funds stay protected while the transaction conditions are being completed.
3. The money is only released when the agreed requirements are met.

This reduces:

- Fraud risk
- Payment disputes
- Unauthorized release of funds
- Unclear transaction records

### Key Benefits

- Secure property buying and selling
- Transparent offer and reservation process
- Protected payments through escrow
- Construction payments linked to approved progress
- Clear role-based access for each user type
- Reliable audit trail for important actions

### Security Overview

NyumbaTrust is designed with strong controls to protect users and funds.

Key protections include:

- Secure sign-in
- Role-based access control
- Escrow protection before funds are released
- Approval-based payment workflows
- Verified payment event handling
- Audit-friendly transaction records

> Important Note: NyumbaTrust is designed to ensure that no payment is released without the required approvals and system checks.

## 2. User Roles & Permissions

Each user sees features based on their assigned role. This helps protect transactions and keeps responsibilities clear.

### BUYER

**Can:**

- Search for available properties
- View published property listings
- Submit offers for properties
- Withdraw their own offers when allowed
- View their own reservations
- View their own escrow records related to purchases

**Cannot:**

- Publish property listings
- Accept or reject offers
- Approve construction milestones
- Access another user’s transaction details

### SELLER

**Can:**

- Create property listings
- Update their own listings
- Publish their own listings
- Review offers received on their properties
- Accept or reject offers on their own properties
- View reservations linked to their listings

**Cannot:**

- Access another seller’s property records
- Submit offers as a buyer unless separately assigned that role
- Approve construction milestones unless also assigned as OWNER

### OWNER

This role refers to the owner of a construction project or asset.

**Can:**

- Create and manage construction projects
- Assign contractor and inspector roles to a project
- Create milestones
- Approve milestones
- Trigger retention release when allowed
- Monitor project progress and payment flow

**Cannot:**

- Access projects they do not own
- Approve other owners’ projects
- Submit contractor evidence unless also assigned as CONTRACTOR

### CONTRACTOR

**Can:**

- View assigned projects
- View assigned milestones
- Submit milestone evidence
- Track payment status for completed work

**Cannot:**

- Approve milestones
- Release funds directly
- Access projects not assigned to them

### INSPECTOR

**Can:**

- View assigned projects and inspections
- Conduct and complete inspections
- Record inspection outcomes
- Support milestone review decisions

**Cannot:**

- Approve payments directly unless allowed by business process
- Access projects not assigned to them
- Release escrow funds

### ADMIN

**Can:**

- Monitor escrows
- Monitor disbursements and payout activity
- View webhook/payment event status
- Review system activity and audit records
- Support operational issue handling
- Manage platform oversight functions

**Cannot:**

- Override security controls outside approved processes
- Act as another user without authorization

> Important Note: Access is limited to what each role needs. Users cannot view or act on records that belong to someone else unless their role specifically allows it.

## 3. Getting Started

### Creating an Account

1. Open the NyumbaTrust sign-in page.
2. Select the account registration option, if available.
3. Enter your required details:
- Full name
- Email address
- Phone number
- Password
4. Confirm your details.
5. Submit the registration form.
6. Complete any verification steps if prompted.

### Logging In

1. Open the login page.
2. Enter your email or registered identifier.
3. Enter your password.
4. Select **Log In**.
5. You will be redirected to your dashboard.

If you have more than one role, your dashboard may show features based on your assigned permissions.

### Dashboard Overview

The dashboard is your main workspace. It gives quick access to your tasks, records, and status updates.

Common dashboard items include:

- My properties
- My offers
- My reservations
- My escrows
- My projects
- My milestones
- Notifications
- Account settings

[SCREENSHOT: Main Dashboard]

### Navigation Explanation

The main navigation menu typically includes:

- Dashboard
- Marketplace
- Escrow
- Construction
- Notifications
- Admin Panel (for admin users)
- Profile / Settings

Use the menu to move between modules without leaving the platform.

### Role-Based Dashboard Explanation

Different roles may see different dashboard widgets.

**Buyer dashboard may show:**

- Available properties
- Active offers
- Reservation status
- Purchase escrow updates

**Seller dashboard may show:**

- Property listings
- Incoming offers
- Reservation activity

**Owner dashboard may show:**

- Project overview
- Pending milestone approvals
- Payment and retention updates

**Contractor dashboard may show:**

- Assigned milestones
- Evidence submission tasks
- Payout tracking

**Inspector dashboard may show:**

- Scheduled inspections
- Pending inspections
- Inspection history

**Admin dashboard may show:**

- Escrow activity
- Payout queues
- Alerts and exceptions
- System monitoring data

## 4. Marketplace Module

### A) Creating a Property Listing

1. Open the **Marketplace** section.
2. Select **Create Property**.
3. Enter listing details:
- Property title
- Description
- Location
- Asking price
- Currency
4. Save the listing as a draft.

This allows you to review the details before making the listing visible to buyers.

[SCREENSHOT: Property Creation Form]

### B) Publishing a Property

1. Open **My Properties**.
2. Select the draft property.
3. Review all information for accuracy.
4. Select **Publish**.

Once published:

- The property becomes searchable
- Buyers can submit offers
- The listing appears in the marketplace

> Important Note: Only the owner or seller of the property can publish that listing.

### C) Searching for Properties

1. Open the **Marketplace** section.
2. Use filters such as:
- Price range
- Location
- Status
3. Browse the available property results.
4. Open a property to view more details.

[SCREENSHOT: Property Search Page]

### D) Submitting an Offer

1. Open the property listing.
2. Select **Submit Offer**.
3. Enter your offer amount.
4. Add notes if needed.
5. Review your submission.
6. Confirm the offer.

After submission:

- The seller is notified
- The offer receives a status
- You can track the result from your dashboard

### E) Accepting / Rejecting Offers

For sellers:

1. Open **Incoming Offers**.
2. Select the offer to review.
3. Check:
- Buyer details
- Offer amount
- Related property
4. Choose one action:
- **Accept**
- **Reject**

If the offer is accepted:

- A reservation is created
- The next payment step may begin

If the offer is rejected:

- The buyer is notified
- The offer is closed

### F) Reservation Creation

When an offer is accepted, the system creates a reservation to temporarily hold the property for the buyer.

A reservation helps:

- Prevent double booking
- Reserve the property during payment preparation
- Track the transaction timeline

### G) Reservation Expiration Logic

Reservations may expire if the buyer does not complete the required next step within the allowed time.

Typical reasons for expiration:

- Escrow was not funded on time
- Required confirmation was not completed
- The reservation period ended

After expiration:

- The property may become available again
- The seller may accept other offers
- The buyer may need to submit a new offer

[SCREENSHOT: Reservations List]

> Important Note: A reservation does not always mean the sale is complete. It usually means the property is temporarily held while the transaction continues.

## 5. Escrow Module

### What Is Escrow?

Escrow is a secure holding process for money during a transaction.

Instead of paying the seller directly:

1. The buyer places funds into escrow.
2. The money is held safely.
3. Funds are released only after the platform confirms the required conditions have been met.

This protects both sides.

### Escrow Lifecycle

NyumbaTrust uses a clear escrow status flow:

**INITIATED**

- Escrow has been created
- The transaction has started
- Funding is still pending

**FUNDED**

- The buyer has sent money into escrow
- The funds have been received or confirmed

**ACTIVE**

- Escrow is in progress
- Conditions are being fulfilled
- The transaction is still under controlled review

**COMPLETED**

- The transaction conditions are met
- Funds have been successfully released
- The escrow process is closed

### How Buyer Funds Escrow

1. Open the related escrow record.
2. Review:
- Amount due
- Currency
- Reference details
3. Follow the payment instructions shown in the platform.
4. Complete the payment using the approved method.
5. Wait for the platform to confirm the funding update.

[SCREENSHOT: Escrow Funding Instructions]

### What Happens When Escrow Is Funded

After the platform confirms funding:

- The escrow status updates
- The transaction can proceed to the next stage
- The seller and other relevant parties may receive a notification
- Funds remain protected until release conditions are met

### How Funds Are Released

Funds are released only when the required transaction conditions are satisfied.

Examples:

- A property sale reaches the approved release stage
- A construction milestone is approved
- A retention release becomes due

The release process is controlled and recorded by the platform.

### Dispute Handling

If dispute management is available in your organization’s process:

- A transaction may be paused for review
- Funds may remain held until the issue is resolved
- Support or admin staff may review the case
- Additional documents or confirmations may be requested

### Viewing Disbursements

Users with permission can view related disbursements to track outgoing payments.

A disbursement record may show:

- Recipient
- Amount
- Status
- Reference number
- Processing date

[SCREENSHOT: Disbursement History]

### Understanding Statuses

Common escrow-related statuses include:

- Initiated
- Funded
- Active
- Completed
- Pending review
- Delayed
- Failed (if a payout or confirmation does not complete successfully)

> Important Note: A funded escrow does not mean the seller has already received the money. It means the funds are securely held until the required conditions are met.

## 6. Construction Module

### Creating a Project

For project owners:

1. Open the **Construction** section.
2. Select **Create Project**.
3. Enter project details:
- Project title
- Description
- Budget
- Retention rate (if applicable)
4. Save the project.

[SCREENSHOT: Create Project Form]

### Assigning Contractor and Inspector

1. Open the project record.
2. Select **Assign Participants**.
3. Choose:
- Contractor
- Inspector
4. Confirm the assignment.

After assignment:

- The contractor can work on assigned tasks
- The inspector can perform required inspections

### Creating Milestones

Milestones break the project into payment stages.

1. Open the project.
2. Select **Create Milestone**.
3. Enter:
- Milestone name
- Description
- Sequence or order
- Total amount
- Due date
- Retention amount or rate, if required
4. Save the milestone.

[SCREENSHOT: Milestone Setup]

### Submitting Milestone Evidence

For contractors:

1. Open the assigned milestone.
2. Select **Submit Evidence**.
3. Upload or enter the required proof of work.
4. Add notes if needed.
5. Submit for review.

Examples of evidence:

- Photos
- Progress notes
- Completion documents
- Site records

### Requesting Inspection

If inspection is part of the workflow:

1. Open the project or related milestone.
2. Select the inspection option.
3. Confirm the inspection request.
4. Wait for assignment or scheduling confirmation.

The inspector can then review the work and record the outcome.

### Approving a Milestone

For project owners:

1. Open the milestone.
2. Review submitted evidence.
3. Review any inspection results.
4. Confirm whether the milestone meets the required standard.
5. Select **Approve**.

After approval:

- Payment processing may begin
- The milestone status changes
- Retention may be held automatically

> Important Note: Approval should only be completed after all required evidence and checks are satisfactory.

### Multi-Party Payout Explanation

Some milestones may pay more than one party.

For example:

- Main contractor
- Inspector
- Supplier
- Platform fee account

In these cases:

- The total approved milestone amount is split according to the payment setup
- Each recipient receives their assigned portion
- The platform tracks each payout separately

This helps ensure all approved participants are paid correctly.

### Retention Hold Explanation

Retention is a portion of the milestone payment that is held back temporarily.

It acts as a safeguard:

- To cover defects
- To ensure final completion
- To encourage quality and accountability

The retained amount is not paid immediately, even if the milestone is approved.

### Retention Release Timing

Retention is released later, based on the platform rules or project schedule.

Typical release conditions:

- The milestone has already been paid
- The retention release date has been reached
- No unresolved issues block release

Once eligible:

- The retained funds can be released to the appropriate party
- The release is recorded in the system

### Financial Logic in Simple Terms

**Gross Amount**

- The full approved amount for a milestone before any holdback

**Retention**

- The part of the gross amount kept back temporarily

**Net Payout**

- The amount paid now after retention is deducted

Simple example:

- Gross Amount: 1,000
- Retention: 100
- Net Payout: 900

Formula:

- Net Payout = Gross Amount - Retention

[SCREENSHOT: Milestone Payment Breakdown]

## 7. Notifications & Status Indicators

### Offer Status Meaning

Common offer statuses include:

- Submitted
- Accepted
- Rejected
- Withdrawn
- Expired

**What they mean:**

- **Submitted:** The offer has been sent to the seller.
- **Accepted:** The seller approved the offer.
- **Rejected:** The seller declined the offer.
- **Withdrawn:** The buyer cancelled their own offer.
- **Expired:** The offer is no longer active.

### Escrow Status Meaning

Common escrow statuses include:

- Initiated
- Funded
- Active
- Completed
- On hold
- Delayed

**What they mean:**

- **Initiated:** Escrow has been opened.
- **Funded:** Funds have been received.
- **Active:** The transaction is in progress.
- **Completed:** Funds were released and the process is closed.
- **On hold:** The transaction is paused for review.
- **Delayed:** The process is taking longer than expected.

### Milestone Status Meaning

Common milestone statuses include:

- Planned
- Submitted
- Under review
- Approved
- Paid
- Retention held
- Retention released

**What they mean:**

- **Planned:** Milestone is created but work is not yet submitted.
- **Submitted:** Contractor has provided evidence.
- **Under review:** The owner or inspector is reviewing the submission.
- **Approved:** The milestone has been accepted.
- **Paid:** The payment has been released for the payable amount.
- **Retention held:** A portion is being held back.
- **Retention released:** The held amount has been released.

### Color Coding

If color indicators are enabled in your interface, a common pattern may be:

- **Green:** Completed, approved, funded, successful
- **Blue:** Active, submitted, in progress
- **Yellow / Amber:** Pending, under review, attention needed
- **Red:** Rejected, failed, delayed, blocked
- **Gray:** Draft, inactive, expired, archived

[SCREENSHOT: Status Indicators Panel]

> Important Note: Always read the text label as the final source of meaning. Colors help visibility but should not be the only reference.

## 8. Admin Panel

### Monitoring Escrows

Administrators can:

- View active escrows
- Track escrow status changes
- Review funding and completion progress
- Identify stalled transactions

### Monitoring Payouts

Administrators can:

- Review pending disbursements
- Track completed and delayed payouts
- Identify payment issues needing follow-up
- Verify whether recipients were processed correctly

### Monitoring Webhook Events

Administrators and operations teams may monitor payment event updates from connected payment systems.

This helps:

- Confirm payment updates were received
- Detect delayed updates
- Review failed or repeated events
- Support operational follow-up

### Handling Disputes

Where dispute handling is part of operations:

1. Review the affected transaction
2. Gather supporting documents
3. Check the current status
4. Coordinate with the relevant parties
5. Apply the approved dispute process

### Viewing Audit Logs

Audit records help administrators and management review:

- Who performed an action
- What action was taken
- When it happened
- Which record was affected

This supports:

- Internal controls
- Compliance needs
- Issue investigation
- Operational accountability

[SCREENSHOT: Admin Monitoring Dashboard]

> Important Note: Administrative access should be limited to authorized staff only.

## 9. Security & Data Protection

### Secure Sign-In (Simple Explanation)

NyumbaTrust uses secure sign-in sessions so users can access the platform safely after login.

In simple terms:

- You log in once with your credentials
- The platform confirms your identity
- You are given a secure session to use the system
- If your session ends, you must log in again

This helps keep unauthorized users out.

### Role-Based Access

The platform checks what each user is allowed to see and do.

This means:

- Buyers only see their own purchase-related actions
- Sellers only manage their own properties
- Contractors only access assigned work
- Inspectors only access assigned inspections
- Admins have oversight tools, not unrestricted personal access to every action

### Escrow Protection

NyumbaTrust protects funds by:

- Holding money securely until conditions are met
- Preventing early release
- Requiring required approvals and transaction checks
- Recording important money movement events

### Ledger Integrity (High-Level Explanation)

NyumbaTrust keeps a protected financial record trail.

In simple terms:

- Each financial event is recorded in sequence
- Records are linked in a way that supports review and verification
- This makes it easier to detect tampering or inconsistencies

This supports trust, control, and audit readiness.

### Webhook Protection

When external payment updates are received:

- The platform checks that the update is genuine
- Suspicious or duplicate updates are not processed automatically
- This helps protect against false payment updates

> Important Note: Never share your password or allow another person to use your account. Contact support immediately if you suspect unauthorized access.

## 10. Frequently Asked Questions (FAQ)

### 1. What happens if the buyer does not fund escrow?

- The transaction may remain pending
- The reservation may expire after the allowed time
- The seller may be able to proceed with other buyers

### 2. Can a contractor be paid without inspection?

- This depends on the project workflow
- If inspection is required, payment should not proceed until review is complete
- If inspection is optional, the owner may approve based on the configured process

### 3. What if a milestone fails inspection?

- The milestone may remain under review or be sent back for correction
- The contractor may need to resubmit evidence
- Payment may be delayed until the issue is resolved

### 4. How is retention calculated?

- Retention is a percentage or amount held back from the gross milestone amount
- It is set by the project or milestone rules
- The retained amount is released later if conditions are met

### 5. Can escrow be cancelled?

- In some cases, yes
- Cancellation depends on the transaction stage and platform rules
- Contact support or the responsible admin team if the normal cancellation option is not available

### 6. When does a seller receive payment?

- Payment is released only after the required transaction conditions are met
- A funded escrow alone does not mean the seller has already been paid

### 7. Can I edit a property after publishing it?

- You may be able to update allowed details depending on your permissions and transaction stage
- Some changes may be restricted once offers are active

### 8. Can I submit more than one offer on a property?

- This depends on platform rules and the current property status
- Check the property page and your current offer history

### 9. How do I know if my milestone was approved?

- Check the milestone status in the Construction module
- You may also receive a notification in your dashboard

### 10. Why is part of my payment missing?

- A retention amount may have been held back
- In some cases, the payment may be split among multiple recipients
- Review the milestone payment breakdown for details

### 11. Can a reservation expire even after my offer is accepted?

- Yes
- If required payment steps are not completed in time, the reservation can expire

### 12. Who can see audit or transaction records?

- Only users with the required permission level, such as administrators or authorized operations staff

## 11. Troubleshooting Guide

### Login Problems

**Possible causes:**

- Incorrect email or password
- Expired session
- Account not active
- Wrong login page or profile

**What to do:**

1. Re-enter your details carefully.
2. Check for typing errors.
3. Reset your password if that option is available.
4. Try logging in again.
5. Contact support if the issue continues.

### Offer Not Visible

**Possible causes:**

- The offer was not submitted successfully
- The listing is no longer active
- You are signed into the wrong account
- The page needs refreshing

**What to do:**

1. Refresh the page.
2. Check your offers section.
3. Confirm you are using the correct account.
4. Contact support if the offer still does not appear.

### Escrow Not Updating

**Possible causes:**

- Payment confirmation is still processing
- The payment reference was incorrect
- There is a delay in status update
- Additional review is needed

**What to do:**

1. Wait a few minutes and refresh.
2. Confirm the payment was completed correctly.
3. Check your escrow status page.
4. Contact support with your transaction reference.

### Payment Delayed

**Possible causes:**

- Approval is still pending
- Inspection is incomplete
- Payment processing is delayed
- Retention is being held

**What to do:**

1. Check the current milestone or escrow status.
2. Review whether inspection is pending.
3. Confirm whether retention applies.
4. Contact support if the delay exceeds normal processing time.

### Inspection Not Assigned

**Possible causes:**

- The project is incomplete
- An inspector has not yet been assigned
- The request is still pending review

**What to do:**

1. Check project details.
2. Confirm an inspector has been assigned.
3. Review any pending actions from the owner.
4. Contact operations support if no assignment appears.

### Property Not Showing in Search

**Possible causes:**

- The listing is still in draft
- Required fields are incomplete
- Filters are too narrow

**What to do:**

1. Confirm the property is published.
2. Check listing details.
3. Clear search filters and search again.

### Milestone Cannot Be Approved

**Possible causes:**

- Evidence was not submitted
- Inspection is pending
- Required information is incomplete
- You do not have the correct role

**What to do:**

1. Review milestone details.
2. Confirm all required evidence is present.
3. Check whether inspection is complete.
4. Confirm you are using the correct account.

> Important Note: If you contact support, include the record ID, date, and a brief description of the issue. This helps speed up resolution.

## 12. Support & Contact

For help with NyumbaTrust, use the approved support channels below.

**Customer Support**

- Email: [support@yourcompany.com]
- Phone: [Support Phone Number]
- Hours: [Business Hours]

**Operations Team**

- Email: [operations@yourcompany.com]

**Admin / Escalations**

- Email: [admin@yourcompany.com]

**Office Address**

- [Company Address]

**Website**

- [www.yourcompany.com]

When contacting support, provide:

- Your full name
- Registered email address
- User role
- Transaction, project, property, or escrow reference
- A short summary of the issue

## Final Notes

- Always review details before confirming an action.
- Keep your login credentials private.
- Use the correct account for your role.
- Follow the status indicators to understand what action is needed next.
- Contact support promptly if something looks incorrect.

> Important Note: NyumbaTrust is designed to provide secure, transparent, and accountable handling of property transactions and construction payments. Following the correct workflow helps protect all parties involved.
