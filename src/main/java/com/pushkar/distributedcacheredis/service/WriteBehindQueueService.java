package com.pushkar.distributedcacheredis.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.pushkar.distributedcacheredis.entity.Product;

import jakarta.annotation.PreDestroy;

@Service 
public class WriteBehindQueueService {
    private final BlockingQueue<Product> pendingWrites = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService flushExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ProductRepositoryFlusher flusher;
    private static final long FLUSH_INTERVAL_SECONDS = 5;

    public WriteBehindQueueService(ProductRepositoryFlusher flusher) {
        this.flusher = flusher;
        startBackgroundFlush();
    }

    public void enqueue(Product product) {
        pendingWrites.offer(product);
        System.out.println("QUEUED for DB flush: " + product.getId() + " (queue size now: " + pendingWrites.size() + ")");
    }

    private void startBackgroundFlush() {
        flushExecutor.scheduleAtFixedRate(() -> {
            List<Product> batch = new ArrayList<>();
            pendingWrites.drainTo(batch);
            if(!batch.isEmpty()) {
                System.out.println("FLUSHING batch of " + batch.size() + " writes to DB");
                flusher.flushBatch(batch);
            }
        }, FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy 
    public void shutdown() {
        List<Product> remaining = new ArrayList<>();
        pendingWrites.drainTo(remaining);
        if(!remaining.isEmpty()) {
            System.out.println("Final flush on shutdown: " + remaining.size() + " writes");
            flusher.flushBatch(remaining);
        }
        flushExecutor.shutdownNow();
    }
}
