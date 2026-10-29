FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production PORT=8080
COPY package.json ./
RUN npm install --omit=dev --no-audit --no-fund
COPY server.js ./
COPY scripts ./scripts
COPY public ./public
RUN node scripts/vendor.js || true
EXPOSE 8080
USER node
CMD ["node","server.js"]
