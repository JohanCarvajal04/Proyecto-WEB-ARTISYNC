FROM node:24-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN chmod +x docker-entrypoint.sh
EXPOSE 4200
ENTRYPOINT ["./docker-entrypoint.sh"]

