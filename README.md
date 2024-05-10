# OffersService

## Overview

The classified platform such as Craigslist or Avito that provides the opportunity for peer-to-peer exchanges of goods
and services.

Demo video: https://disk.yandex.ru/i/EBx8__tIFzzm3Q

<img src="docs/overview-arch.png" width="600" alt="overview-arch"/>

## How to launch

Run one of the sbt commands below to start the selected component locally with connection to other services in the
Cloud.

```bash
sbt "project service" run
sbt "project bot" run
sbt "project moderation" run
sbt "project parsing" run
```

Before executing the above commands, you need to specify some environment variables:

- ```bash
  export POSTGRES_PASSWORD=<password>
  ```
  for REST API

- ```bash
  export REDIS_PASSWORD=<password>
  ```
  for REST API and Parsing

- ```bash
  export TELEGRAM_TOKEN=<token>
  ```
  for Telegram bot

For example, `sbt "project service" run` will launch the REST API locally, but it will send requests to PostgreSQL and
Redis in the Cloud

### Using Docker

To run the service completely locally, first of all, you need to launch PostgreSQL and Redis:

```bash
docker compose start
```

Specify the required environment variables:

```bash
export POSTGRES_PASSWORD=postgres
export TELEGRAM_TOKEN=<your-token>
```

Then, start the REST-api with "local" parameter:

```bash
sbt "project service" run local
```

Then start other components with the same parameter:

```bash
sbt "project bot" run local
sbt "project moderation" run local
sbt "project parsing" run local
```

Note, that the local Redis does not require a password

## Internals

### Database

#### Actual schema

```sql
create table offers
(
    id          uuid primary key,
    name        text    not null,
    price       integer not null,
    description text    not null,
    status      text    not null,
    source      text default null
);

create table users
(
    id            uuid primary key,
    name          text not null,
    login         text not null unique,
    status        text not null,
    password_hash text not null,
    password_salt text not null
);

create table user_offers
(
    offer_id uuid primary key references offers (id),
    user_id  uuid not null references users (id)
);

create table photos
(
    id          uuid primary key,
    url         text  default null,
    blob        bytea default null,
    telegram_id text  default null
);

create table offer_photos
(
    photo_id uuid primary key references photos (id),
    offer_id uuid not null references offers (id)
);
```

#### Indices

Indices for searching by offers:

```sql
create index offers_name_idx on offers
    using gin (to_tsvector('russian', name));

create index offers_name_description_idx on offers
    using gin (to_tsvector('russian', name || ' ' || description));
```

Indices for internal needs:

```sql
create index photos_offer_id on offer_photos (offer_id);

create index offers_status_idx on offers (status);
```

### REST API

#### Authorization

Each user has login (telegram @login) and password, and, in order to use the service, he should get a session and then
attach it to each request

Creating a session:

<img src="docs/create-session.png" width="600" alt="create-session"/>

Use session in some request:

<img src="docs/use-session.png" width="600" alt="use-session"/>

### Telegram-bot

State machine diagram (without authorization to simplify):

<img src="docs/bot-state-machine.png" alt="bot-state-machine"/>

And authorization separately:

<img src="docs/bot-state-machine-auth.png" width="900" alt="bot-state-machine-auth"/>

### Parsing

Parsing gets offers from youla.ru to fill in the contents of my service

<img src="docs/parsing-arch.png" width="600" alt="parsing-arch"/>

### Moderation

Moderation Worker requests all offers with the `OnModeration` status from the REST API (in batches, with some delay) and
then updating offer statuses to Active or Banned if any violations are detected

<img src="docs/moderation-arch.png" width="450" alt="moderation-arch"/>
