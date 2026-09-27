import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import { fileURLToPath, URL } from "node:url";
import path from "node:path";

const here = fileURLToPath(new URL(".", import.meta.url));
const repoRoot = path.resolve(here, "..");

// envDir points at the repo root so the frontend reads the same .env as the
// backend — one file, no duplicated configuration.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, repoRoot, "VITE_");

  return {
    plugins: [react()],
    envDir: repoRoot,
    resolve: {
      alias: { "@": path.resolve(here, "src") },
    },
    server: {
      port: 5173,
      strictPort: true,
      // Proxying in dev keeps the browser same-origin, which matches how nginx
      // serves the app in production. CORS then only matters for Postman.
      proxy: {
        "/api": {
          target: env.VITE_API_PROXY_TARGET ?? "http://localhost:8080",
          changeOrigin: true,
        },
      },
    },
    build: {
      outDir: "dist",
      sourcemap: true,
    },
  };
});
