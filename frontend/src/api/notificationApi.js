import axiosClient from "./axiosClient.js";

export const notificationApi = {
  list: (page = 0, size = 20) =>
    axiosClient.get("/notifications", { params: { page, size } }).then((r) => r.data),
  unreadCount: () => axiosClient.get("/notifications/unread-count").then((r) => r.data),
  markAllRead: () => axiosClient.post("/notifications/mark-all-read").then((r) => r.data),
};
