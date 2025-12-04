# syntax=docker/dockerfile:1.6

FROM --platform=$BUILDPLATFORM alpine:3.20 AS builder
RUN apk add --no-cache ca-certificates
WORKDIR /src
# COPY . .
# RUN <your-build-commands> && <produce>/app

FROM gcr.io/distroless/base-debian12:nonroot AS runtime
WORKDIR /app
COPY --from=builder /src/app /app/app
USER nonroot
EXPOSE 8080
ENTRYPOINT ["/app/app"]

