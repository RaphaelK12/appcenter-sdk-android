package com.microsoft.appcenter;

import com.microsoft.appcenter.utils.async.AppCenterFuture;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

public class AppCenterStorageTest extends AbstractAppCenterTest {

    @Test
    public void configureValidStorageSizeFromApp() {

        /* Set up storage to succeed resize. */
        when(mChannel.setMaxStorageSize(anyLong())).thenReturn(true);

        /* Configure before start. */
        AppCenterFuture<Boolean> future = AppCenter.setStorageSize(AppCenter.MINIMUM_STORAGE_SIZE);

        /* Verify the operation is still pending. */
        assertFalse(future.isDone());

        /* Since the call is registered, we cannot change our mind anymore. */
        assertFalse(AppCenter.setStorageSize(AppCenter.MINIMUM_STORAGE_SIZE + 1).get());

        /* Start AppCenter. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);

        /* Verify storage size applied. */
        verify(mChannel).setMaxStorageSize(AppCenter.MINIMUM_STORAGE_SIZE);

        /* And result returned to developer. */
        assertTrue(future.get());
    }

    @Test
    public void configureInvalidStorageSizeFromApp() {

        /* Set up storage to succeed resize. */
        when(mChannel.setMaxStorageSize(anyLong())).thenReturn(false);

        /* Configure before start. */
        AppCenterFuture<Boolean> future = AppCenter.setStorageSize(AppCenter.MINIMUM_STORAGE_SIZE - 1);

        /* Verify the operation is immediately failed. */
        assertFalse(future.get());

        /* Configure invalid size does not prevent us from trying again with valid size. */
        configureValidStorageSizeFromApp();
    }

    @Test
    public void cannotConfigureAfterStart() {

        /* Set up storage to succeed resize. */
        when(mChannel.setMaxStorageSize(anyLong())).thenReturn(true);

        /* Start AppCenter. */
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);

        /* Configure after start immediately fails. */
        assertFalse(AppCenter.setStorageSize(AppCenter.MINIMUM_STORAGE_SIZE).get());

        /* Verify default storage size is used. */
        verify(mChannel).setMaxStorageSize(AppCenter.DEFAULT_MAX_STORAGE_SIZE_IN_BYTES);
    }
}
