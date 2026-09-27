package com.rk.fooddelivery.event.notification;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {}
