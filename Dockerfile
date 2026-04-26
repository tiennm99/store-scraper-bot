FROM node:20-alpine

RUN apk --no-cache add tzdata ca-certificates

WORKDIR /app

COPY package.json ./
RUN npm install --omit=dev

COPY src ./src

# Bot uses long polling — no ports exposed.
CMD ["node", "src/index.js"]
