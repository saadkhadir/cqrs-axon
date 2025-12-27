# CQRS-Axon — Rapport technique complet

[![Java](https://img.shields.io/badge/Java-21-blue)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Axon](https://img.shields.io/badge/Axon-4.x-orange)](https://axoniq.io/)

Table des matières
- Résumé
- Objectifs du projet
- Technologies & dépendances clés
- Vue d'ensemble de l'architecture
- Organisation du dépôt (modules et rôle)
- Modèle de domaine (commands/events/entities)
- Séquences d'exécution (scénarios) et flux d'événements
- Modèle de données pour les projections
- Configuration et variables d'environnement
- Guide de développement local (build, run, debugging)
- Endpoints REST & exemples curl
- Tests et stratégie de validation
- Observabilité et bonnes pratiques en production
- Résolution des problèmes fréquents
- Structure des fichiers et cartographie des composants
- Contribution, licences et contacts

---

Résumé
-------
CQRS-Axon est une application démonstrative conçue pour illustrer l'approche CQRS (Command Query Responsibility Segregation) couplée au pattern Event Sourcing en utilisant Axon Framework. Le projet met en œuvre la gestion de comptes bancaires (création, crédit, débit, changement de statut) du côté "command" (agrégats, commandes) et la construction de projections analytiques via un service dédié "analytics-service".

Objectifs du projet
--------------------
- Montrer une séparation claire entre la modélisation des commandes (mutations) et des requêtes (lectures).
- Utiliser Axon pour l'apprentissage de l'Event Sourcing et des Event Handlers/projections.
- Fournir un microservice de projection léger (analytics-service) illustrant comment consommer et transformer des événements pour un read model dédié.
- Présenter des bonnes pratiques : idempotence des projections, tests d'agrégats, configuration des serializers, et déploiement local.

Technologies & dépendances clés
-------------------------------
- Java 21
- Spring Boot 4.x
- Axon Framework 4.x
- Maven
- JPA / Hibernate
- PostgreSQL (command side recommandé pour persistance d'événements en prod)
- H2 (analytics-service, in-memory par défaut pour la démo)
- Lombok

Vue d'ensemble de l'architecture
--------------------------------
Le dépôt suit une architecture mono-repo multi-modules :
- Module racine (cqrs-axon) : service de commandes (agrégats, commandes, controllers REST) qui applique les événements et les publie dans l'Event Store.
- Module analytics-service : microservice séparé abonné aux événements pour construire et maintenir un read model (AccountAnalytics) via JPA.

Composants principaux et flux :
- Command API (REST) → CommandGateway → Aggregate (enforce invariants) → apply(Event)
- Event Store (persistant) ← évènements appliqués
- Projections / EventHandlers (analytics-service) ← s'abonnent aux événements, mettent à jour la base JPA de lecture

Schéma simplifié :

Command service (Aggregate)
  ├─ Commands (CreateAccount, DebitAccount, CreditAccount, UpdateStatus)
  ├─ AccountAggregate (EventSourcingHandler)
  └─ Emits Events -> Event Store
                 │
                 ▼
Analytics service (Projections)
  └─ AccountAnalytics (JPA) — read model mis à jour en temps réel

Organisation du dépôt — modules et responsabilités
-------------------------------------------------
- / (root - cqrs-axon)
  - src/main/java/org/example/cqrsaxon
    - command/controllers — REST controllers exposant les endpoints pour émettre des commandes
    - command/aggregates — AccountAggregate (logique métier, EventSourcing handlers)
    - command/commands — classes Command
    - commons/events — Java records des événements partagés
    - commons/dtos / enums — DTOs et énumérations partagées
    - query/* — (optionnel) composants de lecture si inclus
  - resources/application.properties — configuration globale pour le command side

- analytics-service/
  - src/main/java/org/example/analyticsservice/
    - service — AccountAnalyticsEventHandler (EventHandlers et QueryHandlers)
    - entities — AccountAnalytics (entité JPA pour la projection)
    - repo — AccountAnalyticsRepository (Spring Data JPA)
    - controller — endpoints / queries
  - resources/application.properties — configuration de l'analytics-service (H2 par défaut)

Modèle de domaine (commands / events / read models)
--------------------------------------------------
Commands (exemples)
- CreateAccountCommand(id, initialBalance, currency)
- DebitAccountCommand(id, amount)
- CreditAccountCommand(id, amount)
- UpdateAccountStatusCommand(id, accountStatus)

Events (Java records — attention aux accesseurs)
- AccountCreatedEvent(accountId, initialBalance, currency, accountStatus)
- AccountDebitedEvent(accountId, amount)
- AccountCreditedEvent(accountId, amount)
- AccountStatusUpdatedEvent(accountId, fromStatus, toStatus)

Important : les événements sont définis comme des Java records. Leurs accesseurs sont nommés d'après les composants (ex : accountId(), amount(), initialBalance()). N'utilisez pas getX() sur un record.

Read model (analytics)
- AccountAnalytics { id (PK), accountId, balance, totalDebit, totalCredit, totalNumberOfDebits, totalNumberOfCredits }

Séquences d'exécution (scénarios détaillés)
-------------------------------------------
1) Création de compte
- Étape 1 : Client POST /commands/accounts/create (payload CreateAccountDTO)
- Étape 2 : Controller convertit en CreateAccountCommand et l'envoie via CommandGateway
- Étape 3 : AccountAggregate (constructor handler) vérifie les invariants et applique AccountCreatedEvent
- Étape 4 : EventStore persiste l'événement
- Étape 5 : AccountAnalyticsEventHandler (dans analytics-service) reçoit AccountCreatedEvent et crée une entrée AccountAnalytics

2) Débit d'un compte
- Étape 1 : Client POST /commands/accounts/debit
- Étape 2 : Controller envoie DebitAccountCommand
- Étape 3 : Aggregate vérifie état (status must be ACTIVATED) et solde suffisant ; applique AccountDebitedEvent
- Étape 4 : analytics-service reçoit l'événement et met à jour la projection (balance, totalDebit, totalNumberOfDebits)

3) Crédit d'un compte — similaire au débit mais en sens inverse
4) Mise à jour du statut — applique AccountStatusUpdatedEvent ; les projections réagissent selon besoin

Modèle de données pour les projections
--------------------------------------
Entité AccountAnalytics (JPA)
- id : Long (PK auto-generated)
- accountId : String (partition key logique)
- balance : double
- totalDebit : double
- totalCredit : double
- totalNumberOfDebits : int
- totalNumberOfCredits : int

Indexation recommandée (production)
- index sur accountId pour requêtes rapides
- stockage partitionné / sharding pour très grand volume d'événements

Configuration et variables d'environnement
-----------------------------------------
Fichiers de configuration principaux :
- /src/main/resources/application.properties (command side)
  - spring.datasource.url (Postgres recommended in prod)
  - spring.jpa.hibernate.ddl-auto
  - axon.serializer.* (jackson pour events, xstream for messages in this project)

- analytics-service/src/main/resources/application.properties
  - spring.datasource.url=jdbc:h2:mem:analyticsdb (démo)
  - axon.serializer.*

Variables d'environnement utiles
- DB_URL — override pour la base de données postgres
- SPRING_PROFILES_ACTIVE — ex : prod, dev

Guide de développement local
----------------------------
Prérequis
- Java 21
- Maven
- (optionnel) PostgreSQL pour persistance d'Event Store

Étapes rapides
1. Compiler et installer l'artifact partagé (nécessaire pour analytics-service)
   mvn -DskipTests install

2. Lancer analytics-service en mode développement (H2)
   mvn -pl analytics-service -am -DskipTests spring-boot:run

3. Lancer le command service (racine / cqrs-axon)
   mvn -DskipTests spring-boot:run

Conseils IDE (IntelliJ / Eclipse)
- Après mvn install, recharger tous les projets Maven (Reload All Maven Projects)
- Si des symboles restent non résolus : File → Invalidate Caches / Restart (IntelliJ)

Endpoints REST & exemples
-------------------------
Command API (Command service — port par défaut : 8787)
- POST /commands/accounts/create
  Payload: { "initialBalance": 1000, "currency": "USD" }

- POST /commands/accounts/debit
  Payload: { "accountId": "<id>", "amount": 100 }

- POST /commands/accounts/credit
  Payload: { "accountId": "<id>", "amount": 50 }

- PUT /commands/accounts/updateStatus
  Payload: { "accountId": "<id>", "accountStatus": "ACTIVATED" }

Query / Analytics API (analytics-service — port par défaut : 8084)
- GET /query/accountAnalytics
- GET /query/accountAnalytics/{accountId}

Exemples curl
- Create account
  curl -s -X POST http://localhost:8787/commands/accounts/create \
    -H "Content-Type: application/json" \
    -d '{"initialBalance":1000,"currency":"USD"}'

- Debit
  curl -s -X POST http://localhost:8787/commands/accounts/debit \
    -H "Content-Type: application/json" \
    -d '{"accountId":"<id>","amount":100}'

- Query analytics
  curl -s http://localhost:8084/query/accountAnalytics

Tests et stratégie de validation
-------------------------------
- Unit tests (agrégats)
  - Tester les CommandHandlers : envoyer une commande et vérifier que l'événement attendu est appliqué.
- Tests d'intégration
  - Démarrer l'application Spring Boot, envoyer des commandes via REST, vérifier que les projections sont mises à jour.
- Tests de non-régression
  - Vérifier l'idempotence des EventHandlers : relecture d'événements ne doit pas créer d'incohérences dans le read model.

Observabilité & bonnes pratiques en production
---------------------------------------------
- Event Store persistant (Postgres ou Axon Server) : ne pas utiliser H2 pour la persistance d'événements en prod.
- Serializers
  - Utiliser Jackson pour les événements pour garder la compatibilité JSON.
  - Versionner les événements si vous prévoyez des évolutions du schéma.
- Monitoring
  - Logs structurés (JSON) et corrélation d'ID de commande/trace.
  - Exportez métriques (Prometheus) et traces (OpenTelemetry).
- Résilience
  - Retours d'erreurs idempotents sur EventHandlers.
  - Gestion des dead-letter / retries pour les handlers asynchrones.

Résolution des problèmes fréquents
----------------------------------
- "Cannot resolve symbol AccountCreditedEvent" ou autres événements
  - Cause : module partagé non installé localement
  - Solution : exécutez `mvn -DskipTests install` à la racine, puis rechargement Maven dans l'IDE.

- "Cannot resolve method getId() / getAmount() on event"
  - Cause : les événements sont des Java records. Les accesseurs sont accountId(), amount(), initialBalance().
  - Solution : remplacer event.getId() par event.accountId(), event.getAmount() par event.amount(), etc.

- Erreurs Axon liées aux serializers
  - Vérifiez les propriétés `axon.serializer.events` et `axon.serializer.general` dans application.properties.

- Problèmes d'EventHandler non invoqués en local
  - Assurez-vous que le subscriber/processor Axon est bien configuré et que le bus d'événements est partagé/accessible entre services (si multi-instance).

Structure des fichiers — cartographie rapide
------------------------------------------
- Evénements : src/main/java/org/example/cqrsaxon/commons/events/
- Agrégat principal : src/main/java/org/example/cqrsaxon/command/aggregates/AccountAggregate.java
- Command controllers : src/main/java/org/example/cqrsaxon/command/controllers/AccountCommandController.java
- Projections analytics : analytics-service/src/main/java/org/example/analyticsservice/service/AccountAnalyticsEventHandler.java
- Entité projection : analytics-service/src/main/java/org/example/analyticsservice/entities/AccountAnalytics.java

Conseils de design et améliorations possibles
--------------------------------------------
- Introduire Axon Server pour centraliser l'Event Store et la distribution d'événements entre microservices.
- Ajouter versioning d'événements (headers / metadata) pour faciliter l'évolution du schéma.
- Implémenter mappers/DTOs dédiés entre événements et entités JPA pour désengorger les EventHandlers.
- Ajouter une couche de sécurité (JWT / OAuth2) pour protéger les endpoints Command & Query.
- Remplacer H2 par une base SQL managée pour l'analytics en production et configurer backup/restore.

