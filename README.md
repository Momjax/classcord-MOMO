ClassCord Client – Projet BTS SIO SLAM 2025
Auteur : Mohamed BOUKHATEM
Option : BTS SIO - SLAM
Dépôt GitHub : https://github.com/Momjax/classcord-MOMO

Description
ClassCord est un client de messagerie instantanée développé en Java Swing dans le cadre du BTS SIO option SLAM. Il se connecte à un serveur ClassCord via TCP et échange des messages au format JSON. L’application supporte :

Inscription et connexion avec identifiants

Chat global et chat privé

Connexion en invité avec pseudo libre

Affichage dynamique de la liste des utilisateurs connectés avec gestion des statuts

Gestion des erreurs et retours du serveur

Fonctionnalités principales
Interface graphique Swing avec fenêtres dédiées :

LoginFrame : connexion / inscription

RegisterFrame : inscription utilisateur

ChatFrame : chat avec affichage des messages et liste des utilisateurs

Communication TCP via JSON (classe ClientInvite)

Thread dédié à la réception asynchrone des messages

Envoi de messages globaux et privés avec différenciation visuelle

Affichage dynamique et mise à jour des statuts utilisateurs (disponible, absent, invisible)

Gestion de la connexion en invité

Gestion des erreurs serveur (nom utilisé, erreurs JSON, échec connexion)

Structure du projet
fr.classcord.model : classes métier (User, Message, etc.)

fr.classcord.network : gestion de la connexion réseau (ClientInvite)

fr.classcord.ui : interfaces graphiques Swing

fr.classcord.app : classe principale et démarrage

Installation et configuration
Prérequis
Java JDK 17 ou supérieur

Maven installé sur la machine

Bibliothèque JSON (org.json) dans le classpath (gérée via Maven)

Serveur ClassCord accessible (adresse IP + port)

Déroulement du projet – Planning détaillé
Jour 1 - Lundi : Mise en place du projet et modélisation
Création du projet Maven sous VSCode

Ajout de la dépendance org.json dans pom.xml

Création de la structure MVC (model, network, ui, app)

Implémentation des classes métier User et Message

Vérification de la compilation et test minimal (Hello ClassCord)

Jour 2 - Mardi : Connexion au serveur et chat en mode invité
Création de la classe ClientInvite pour la connexion TCP

Envoi et réception de messages JSON en mode invité

Affichage des messages reçus dans la console ou UI Swing basique

Thread dédié à la réception des messages pour éviter le blocage

Jour 3 - Mercredi : Authentification et gestion des comptes
Implémentation des fenêtres d'inscription et connexion (Swing)

Envoi des identifiants (login/register) au serveur via JSON

Affichage dynamique des erreurs ou succès reçus

Passage fluide vers la fenêtre de chat après authentification réussie

Jour 4 - Jeudi : Messages privés et liste des utilisateurs
Récupération et affichage dynamique de la liste des utilisateurs connectés avec leur statut

Gestion de l’envoi de messages privés ciblés

Affichage différencié des messages privés et globaux dans l’interface

Jour 5 - Vendredi : Gestion des statuts et finalisation
Ajout d’un menu pour changer le statut utilisateur (disponible, absent, invisible)

Envoi et affichage des statuts dans la liste des utilisateurs

Amélioration graphique de l’interface Swing

Tests complets, correction des bugs

Préparation des livrables techniques et documentation

Utilisation
Lancer le serveur ClassCord (local ou distant) -> Partie SISR

Lancer le client ClassCord via la fenêtre de login

Se connecter avec identifiants ou en invité

Utiliser la fenêtre de chat pour envoyer/recevoir des messages globaux ou privés

Changer son statut via le menu dédié

Observer la liste des utilisateurs connectés mise à jour en temps réel

Difficultés rencontrées
Synchronisation entre threads réseau et Swing (SwingUtilities.invokeLater)

Gestion des multiples types de messages JSON

Passage fluide entre fenêtres (inscription → connexion → chat)

Gestion des fermetures et réouvertures TCP

Affichage dynamique et mise à jour en temps réel des statuts utilisateurs

Contact
Pour toute question, problème ou suggestion, merci de me contacter via GitHub ou mail.

Merci pour votre attention !
Mohamed BOUKHATEM
BTS SIO SLAM 2025

