import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /* /api/* 요청을 admin-api 백엔드로 프록시 */
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:5000/:path*",
      },
    ];
  },
};

export default nextConfig;
