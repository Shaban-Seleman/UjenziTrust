# UjenziTrust System Workflow by Actor

## Purpose

This document explains how the system works from the perspective of each actor, what each actor is responsible for, and how the main business workflows move across modules.

It is intended as a high-level operational guide, not a low-level API specification.

## Core Modules

- Marketplace: property listing, publishing, offer handling, reservation creation
- Escrow: protected transaction and project payment holding logic
- Construction: project setup, participant assignment, milestone tracking, approvals, inspections
- Operations and Monitoring: escrow visibility, disbursement tracking, webhook/event monitoring, audit support

## Actor Map

### BUYER

Primary purpose:
- Finds published properties and makes purchase offers

Core functions:
- Browse published property listings
- View property details
- Submit offers on available properties
- Withdraw offers when allowed
- Track own reservations
- Track own escrows related to purchases

Key workflow position:
- Starts the commercial transaction by expressing intent to buy

### SELLER

Primary purpose:
- Brings property inventory into the system and responds to buyer demand

Core functions:
- Create property records
- Save property as draft
- Review own unpublished and published properties
- Publish property listings
- Review incoming offers
- Accept, reject, or counter offers
- View reservations created from accepted offers

Key workflow position:
- Controls listing readiness and commercial decision-making on offers

### OWNER

Primary purpose:
- Controls construction project setup and approves release of project-linked value

Core functions:
- Create construction projects
- Assign contractor and inspector to a project
- Activate project
- Create project milestones
- Review milestone progress
- Approve milestone payout flows
- Trigger retention release when conditions are met

Key workflow position:
- Governs the construction side after the property/commercial side creates the need for project execution

### CONTRACTOR

Primary purpose:
- Delivers project work and submits proof of milestone completion

Core functions:
- View assigned projects
- View assigned milestones
- Submit milestone evidence
- Track milestone approval and payment status

Key workflow position:
- Moves milestones from planned work into reviewable work

### INSPECTOR

Primary purpose:
- Provides quality and verification support for milestone progress

Core functions:
- View assigned projects
- View assigned milestones
- View or perform inspections
- Record inspection outcomes where applicable
- Support owner decision-making before payout approval

Key workflow position:
- Acts as an independent validation role between execution and approval

### ADMIN

Primary purpose:
- Oversees the platform and resolves operational issues

Core functions:
- Monitor escrows
- Monitor disbursements
- Review webhook/payment event status
- Review outbox processing and system event flow
- Investigate transaction problems
- Support audit and operational control

Key workflow position:
- Provides platform governance and exception handling, not normal business ownership

## End-to-End Workflow

### 1. Property Listing and Offer Workflow

### Step 1: Seller creates a property

Actor:
- SELLER

System behavior:
- Property is created in draft state
- Draft remains visible to the owner of the property but not to public buyers

Outcome:
- Listing exists but is not yet market-visible

### Step 2: Seller publishes the property

Actor:
- SELLER

System behavior:
- Property becomes visible in the marketplace
- Buyers can now discover and act on it

Outcome:
- Listing is open for buyer engagement

### Step 3: Buyer submits an offer

Actor:
- BUYER

System behavior:
- Offer is attached to the target property
- Seller can review it in their offers workflow

Outcome:
- Commercial negotiation begins

### Step 4: Seller responds to the offer

Actor:
- SELLER

System behavior:
- Seller may accept, reject, or counter the offer
- If accepted, the system creates downstream reservation and escrow effects according to the business flow

Outcome:
- Transaction either ends, continues in negotiation, or advances toward protected settlement

### Step 5: Reservation and escrow become active

Actors:
- BUYER
- SELLER
- ADMIN for oversight

System behavior:
- Reservation protects the deal state
- Escrow holds transaction value under controlled release rules

Outcome:
- Funds and transaction state become controlled and traceable

### 2. Construction Project Workflow

### Step 1: Owner creates a project

Actor:
- OWNER

System behavior:
- Project is created as a controlled record for execution tracking

Outcome:
- Construction workflow can begin

### Step 2: Owner assigns participants

Actor:
- OWNER

System behavior:
- Contractor and inspector are assigned to the project
- Assigned users gain access to the project and milestone records for that project

Outcome:
- Role-based execution team is established

### Step 3: Owner activates the project

Actor:
- OWNER

System behavior:
- Project becomes operational for milestone work

Outcome:
- Project is ready for milestone progression

### Step 4: Owner creates milestones

Actor:
- OWNER

System behavior:
- Each milestone defines a sequence, commercial value, and retention amount
- Milestones form the payout and progress checkpoints of the project

Outcome:
- Work is broken into approval-ready units

### Step 5: Contractor submits milestone evidence

Actor:
- CONTRACTOR

System behavior:
- Evidence is attached to a specific milestone
- Milestone state progresses toward review

Outcome:
- Owner and inspector can review fresh milestone-specific completion data

### Step 6: Inspector supports verification

Actor:
- INSPECTOR

System behavior:
- Inspector can view assigned milestone/project records
- Inspection-related workflow supports quality validation before payout decisions

Outcome:
- Milestone review is informed by independent verification

### Step 7: Owner approves milestone payout

Actor:
- OWNER

System behavior:
- Approved milestone triggers controlled disbursement and ledger-related actions
- The milestone remains traceable through payment lifecycle states

Outcome:
- Value is released according to approved progress

### Step 8: Retention release

Actor:
- OWNER

System behavior:
- Retention can be released later when release conditions are met

Outcome:
- Final withheld value is released in a controlled way

### 3. Operations and Monitoring Workflow

### Step 1: Platform events are generated

Actors:
- System
- ADMIN for oversight

System behavior:
- Escrow, disbursement, and webhook-related events are recorded
- Journal and operational event trails are maintained

Outcome:
- Business actions remain auditable

### Step 2: Admin monitors runtime state

Actor:
- ADMIN

System behavior:
- Admin views escrow status, disbursement progress, webhook events, and operational health

Outcome:
- Platform issues can be detected and investigated quickly

### Step 3: Admin supports exception handling

Actor:
- ADMIN

System behavior:
- Admin investigates failed payments, stuck events, unexpected statuses, and support cases

Outcome:
- System reliability is maintained without bypassing business ownership controls

## Access Principles

- Sellers can only manage their own properties and related offers/reservations
- Buyers can only see their own transactional records
- Owners can only manage their own projects and milestones
- Contractors and inspectors can only access projects where they are assigned
- Admins have oversight capability for operational governance
- Draft and unpublished records are not exposed as public marketplace inventory

## Practical View of Actor Responsibilities

### Commercial side

- SELLER supplies property inventory
- BUYER creates buying intent
- Escrow protects both sides once a deal progresses

### Delivery side

- OWNER governs project execution and approval
- CONTRACTOR performs the work
- INSPECTOR validates quality/progress

### Governance side

- ADMIN monitors the platform, transactions, and technical event flow

## Quick Workflow Summary

1. Seller creates and publishes a property
2. Buyer submits an offer
3. Seller accepts and the transaction moves into reservation/escrow protection
4. Owner creates a project tied to execution needs
5. Owner assigns contractor and inspector
6. Owner creates milestones
7. Contractor submits evidence for a milestone
8. Inspector supports validation
9. Owner approves payout
10. System records disbursement and monitoring events
11. Retention is released later when allowed

## Recommended Use of This Document

Use this document for:
- stakeholder onboarding
- product walkthroughs
- implementation planning discussions
- support and operations understanding
- role and access clarification

Use the API docs and codebase for:
- endpoint-level behavior
- payload details
- validation rules
- integration implementation
