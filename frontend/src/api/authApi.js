import axiosClient from "./axiosClient.js";

export const authApi = {
  register: (payload) => axiosClient.post("/auth/register", payload).then((r) => r.data),
  login: (payload) => axiosClient.post("/auth/login", payload).then((r) => r.data),
  logout: (refreshToken) => axiosClient.post("/auth/logout", { refreshToken }),
  profile: () => axiosClient.get("/users/me").then((r) => r.data),
  updateProfile: (payload) => axiosClient.put("/users/me", payload).then((r) => r.data),
  changePassword: (payload) => axiosClient.post("/users/me/change-password", payload),
};
