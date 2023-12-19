# OffersService

## Запуск

### PostgreSQL

Нужен только для `nemia` — сервиса с REST API

```bash
docker run --rm --name offersql \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e PGDATA=/var/lib/postgresql/data/pgdata \
  -v /tmp:/var/lib/postgresql/data \
  -p 5432:5432 -it postgres:14.1-alpine
```

### Redis

Нужен и для `nemia`, и для `parseidon` — парсера сайта объявлений Youla. Для простоты запускаю один инстанс редиса, но при желании можно запустить и два (и прописать разные хосты в конфигах)

Без мониторинга:

```bash
docker run -d --rm --name offers-redis \
  -p 6379:6379 \
  redis/redis-stack-server:latest
```

С мониторингом на http://localhost:8001/redis-stack/browser:

```bash
docker run -d --rm --name offers-redis \
  -p 6379:6379 \
  -p 8001:8001 \
  redis/redis-stack:latest
```

Запуск redis-cli (для дебага):
```bash
docker exec -it offers-redis redis-cli
```
