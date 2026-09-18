.PHONY: dev test integration-test build docker up down logs load-test failure-test clean

dev:
	@docker compose up -d --build

test:
	@cd backend && ./mvnw test

integration-test:
	@cd backend && ./mvnw -Dtest='*IT,*IntegrationTest' test

build:
	@cd backend && ./mvnw -q -DskipTests package
	@cd frontend && npm install && npm run build

docker:
	@docker build -t orderflow-backend ./backend
	@docker build -t orderflow-frontend ./frontend

up:
	@docker compose up -d

down:
	@docker compose down -v

logs:
	@docker compose logs -f

load-test:
	@echo "Benchmark pending; run k6 scripts from load-tests/k6"

failure-test:
	@echo "Failure injection scripts are available under failure-lab/"

clean:
	@rm -rf backend/target frontend/dist frontend/node_modules
