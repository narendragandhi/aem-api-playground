package com.example.aem;

import com.example.aem.security.OAuthManager;
import com.example.aem.operations.BulkOperations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BulkOperationsTest {

    private BulkOperations bulkOperations;

    @BeforeEach
    void setUp() {
        bulkOperations = new BulkOperations(null, 3, 2);
    }

    @AfterEach
    void tearDown() {
        bulkOperations.shutdown();
    }

    @Test
    void testExecuteBulk_Success() throws Exception {
        List<BulkOperations.BulkOperation<Integer>> operations = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int value = i;
            operations.add(() -> value * 2);
        }

        List<BulkOperations.BulkResult<Integer>> results = bulkOperations.executeBulk(operations);

        assertEquals(5, results.size());
        assertTrue(results.stream().allMatch(BulkOperations.BulkResult::isSuccess));
        assertEquals(0, results.get(0).getResult());
        assertEquals(8, results.get(4).getResult());
    }

    @Test
    void testExecuteBulk_WithErrors() {
        List<BulkOperations.BulkOperation<Integer>> operations = new ArrayList<>();
        operations.add(() -> 1);
        operations.add(() -> { throw new RuntimeException("Error"); });
        operations.add(() -> 3);

        List<BulkOperations.BulkResult<Integer>> results = bulkOperations.executeBulk(operations);

        assertEquals(3, results.size());
        assertTrue(results.get(0).isSuccess());
        assertFalse(results.get(1).isSuccess());
        assertNotNull(results.get(1).getError());
        assertTrue(results.get(2).isSuccess());
    }

    @Test
    void testExecuteInBatches() {
        AtomicInteger counter = new AtomicInteger(0);
        List<BulkOperations.BulkOperation<Integer>> operations = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            operations.add(() -> counter.incrementAndGet());
        }

        List<BulkOperations.BulkResult<Integer>> results = bulkOperations.executeInBatches(operations);

        assertEquals(6, results.size());
        assertTrue(results.stream().allMatch(BulkOperations.BulkResult::isSuccess));
    }

    @Test
    void testGetStatistics() {
        List<BulkOperations.BulkOperation<Integer>> operations = new ArrayList<>();
        operations.add(() -> 1);
        operations.add(() -> { throw new RuntimeException("Error"); });
        operations.add(() -> 3);

        List<BulkOperations.BulkResult<Integer>> results = bulkOperations.executeBulk(operations);
        BulkOperations.BulkStatistics stats = bulkOperations.getStatistics(results);

        assertEquals(3, stats.getTotal());
        assertEquals(2, stats.getSuccessCount());
        assertEquals(1, stats.getErrorCount());
        assertEquals(66.67, stats.getSuccessRate(), 0.01);
    }

    @Test
    void testBulkResult() {
        BulkOperations.BulkResult<String> successResult = new BulkOperations.BulkResult<>(1, "test", null);
        assertTrue(successResult.isSuccess());
        assertEquals("test", successResult.getResult());
        assertEquals(1, successResult.getIndex());
        assertNull(successResult.getError());

        BulkOperations.BulkResult<String> errorResult = new BulkOperations.BulkResult<>(2, null, new RuntimeException("error"));
        assertFalse(errorResult.isSuccess());
        assertNotNull(errorResult.getError());
    }
}
