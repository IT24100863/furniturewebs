package com.webs.furniturewebs.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,   // ← Added for better flow
    SHIPPED,
    DELIVERED,
    CANCELLED
}