import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // /api/** 요청을 백엔드로 프록시 (CORS 우회)
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${process.env.API_BASE_URL || "http://localhost:6100"}/:path*`,
      },
    ];
  },
};

export default nextConfig;
