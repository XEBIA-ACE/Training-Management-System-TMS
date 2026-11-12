# User flows

## Coordinator captures requirement and allocates trainer
End-to-end flow for Priya (Training Coordinator) from login to capturing a customer requirement and allocating a qualified trainer, updating the Workshop Dashboard.

### Diagram

```mermaid
graph TD
  classDef persona fill:#fdf2e9,stroke:#333,stroke-width:1px,color:#000
  classDef module fill:#e8f4fd,stroke:#333,stroke-width:1px,color:#000
  classDef external fill:#f5e6ff,stroke:#333,stroke-width:1px,color:#000

  P1[Persona: Training Coordinator (Priya)]
  A[ROOT: ACE_For Academy TMS]
  B[Login & Session]
  C1[Coordinator Home: Workshop Dashboard]
  D3[Customer Management]
  D4[Customer Requirement Management]
  D5[Trainer Allocation Workspace]
  D1[Trainer Management]
  D2[OEM Certification Management]
  X1[Email & Calendar Notifications]

  class P1 persona
  class A,B,C1,D1,D2,D3,D4,D5 module
  class X1 external

  P1 --> A
  A --> B
  B --> C1
  C1 --> D3
  D3 --> D4
  D4 --> D5
  D5 --> D1
  D5 --> D2
  D5 --> X1
  D5 --> C1
```

## Manager reviews upcoming workshops and trainer load
Flow for Rohan (Training Manager) to review the workshop pipeline via dashboard and Trainer Allocation View report, then return to the dashboard.

### Diagram

```mermaid
graph TD
  classDef persona fill:#fdf2e9,stroke:#333,stroke-width:1px,color:#000
  classDef module fill:#e8f4fd,stroke:#333,stroke-width:1px,color:#000
  classDef report fill:#eef7e6,stroke:#333,stroke-width:1px,color:#000

  P2[Persona: Training Manager / Ops Lead (Rohan)]
  A[ROOT: ACE_For Academy TMS]
  B[Login & Session]
  C2[Manager Home: Workshop Dashboard]
  D8[Reports & Dashboards]
  R1[Report: Trainer Allocation View]
  R3[Report: Trainer Feedback Analytics]

  class P2 persona
  class A,B,C2,D8 module
  class R1,R3 report

  P2 --> A
  A --> B
  B --> C2
  C2 --> D8
  D8 --> R1
  D8 --> R3
  R1 --> C2
```

## Super-admin manages users and master data
Flow for Meera (Super-Admin) from login to managing user rights and maintaining trainer, OEM certification, and customer masters.

### Diagram

```mermaid
graph TD
  classDef persona fill:#fdf2e9,stroke:#333,stroke-width:1px,color:#000
  classDef module fill:#e8f4fd,stroke:#333,stroke-width:1px,color:#000

  P3[Persona: System Administrator / Super-Admin (Meera)]
  A[ROOT: ACE_For Academy TMS]
  B[Login & Session]
  C3[Admin Home: Admin & Master Data]
  D9[Admin & User Rights Management]
  D1[Trainer Management]
  D2[OEM Certification Management]
  D3[Customer Management]

  class P3 persona
  class A,B,C3,D1,D2,D3,D9 module

  P3 --> A
  A --> B
  B --> C3
  C3 --> D9
  C3 --> D1
  C3 --> D2
  C3 --> D3
  D9 --> B
```

## Coordinator runs post-workshop feedback and action-item loop
Flow for Priya (Coordinator) to manage training checklists, trigger feedback forms via survey tools, and monitor pending action items from reports.

### Diagram

```mermaid
graph TD
  classDef persona fill:#fdf2e9,stroke:#333,stroke-width:1px,color:#000
  classDef module fill:#e8f4fd,stroke:#333,stroke-width:1px,color:#000
  classDef report fill:#eef7e6,stroke:#333,stroke-width:1px,color:#000
  classDef external fill:#f5e6ff,stroke:#333,stroke-width:1px,color:#000

  P1[Persona: Training Coordinator (Priya)]
  A[ROOT: ACE_For Academy TMS]
  B[Login & Session]
  C1[Coordinator Home: Workshop Dashboard]
  D6[Training Checklist Management]
  D7[Feedback & Workflow]
  D8[Reports & Dashboards]
  R4[Report: Pending Action Items]
  X2[Survey Tools (Google/SM/MS Forms)]
  X3[Customer Emails & Feedback Links]

  class P1 persona
  class A,B,C1,D6,D7,D8 module
  class R4 report
  class X2,X3 external

  P1 --> A
  A --> B
  B --> C1
  C1 --> D6
  D6 --> C1
  C1 --> D7
  D7 --> X2
  D7 --> X3
  D7 --> D8
  D8 --> R4
  R4 --> C1
```

## Trainer receives allocation notifications and calendar blocks
Flow for Arjun (Trainer) interacting primarily via email and calendar notifications when allocations are created or updated in the Trainer Allocation Workspace.

### Diagram

```mermaid
graph TD
  classDef persona fill:#fdf2e9,stroke:#333,stroke-width:1px,color:#000
  classDef module fill:#e8f4fd,stroke:#333,stroke-width:1px,color:#000
  classDef external fill:#f5e6ff,stroke:#333,stroke-width:1px,color:#000

  P4[Persona: Trainer (Arjun)]
  X1[Email & Calendar Notifications]
  A[ROOT: ACE_For Academy TMS]
  B[Login & Session]
  C1[Coordinator Home: Workshop Dashboard]
  D5[Trainer Allocation Workspace]

  class P4 persona
  class A,B,C1,D5 module
  class X1 external

  P4 --> X1
  X1 --> A
  A --> B
  B --> C1
  C1 --> D5
  D5 --> X1
```

## Customer SPOC provides feedback and influences analytics
Flow for Neha (Customer SPOC) responding to feedback links, with responses feeding into Trainer Feedback Analytics reports used by internal users.

### Diagram

```mermaid
graph TD
  classDef persona fill:#fdf2e9,stroke:#333,stroke-width:1px,color:#000
  classDef module fill:#e8f4fd,stroke:#333,stroke-width:1px,color:#000
  classDef report fill:#eef7e6,stroke:#333,stroke-width:1px,color:#000
  classDef external fill:#f5e6ff,stroke:#333,stroke-width:1px,color:#000

  P5[Persona: Customer Stakeholder / Client SPOC (Neha)]
  X3[Customer Emails & Feedback Links]
  X2[Survey Tools (Google/SM/MS Forms)]
  A[ROOT: ACE_For Academy TMS]
  D8[Reports & Dashboards]
  R3[Report: Trainer Feedback Analytics]

  class P5 persona
  class A,D8 module
  class R3 report
  class X2,X3 external

  P5 --> X3
  X3 --> X2
  X2 --> A
  A --> D8
  D8 --> R3
```
