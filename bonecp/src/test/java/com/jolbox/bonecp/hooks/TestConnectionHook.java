/**
 *  Copyright 2010 Wallace Wadge
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/**
 * 
 */
package com.jolbox.bonecp.hooks;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.CommonTestUtils;
import com.jolbox.bonecp.MockConnection;
import com.jolbox.bonecp.MockJDBCAnswer;
import com.jolbox.bonecp.MockJDBCDriver;

/** Tests the connection hooks. 
 * @author wallacew
 *
 */
public class TestConnectionHook {

	/** Mock support. */
	private BoneCPConfig mockConfig;
	/** Pool handle. */
	private BoneCP poolClass;
	/** Helper class. */
	private CustomHook hookClass;
	/** Driver class. */
	private MockJDBCDriver driver;

	/** Setups all mocks.
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws CloneNotSupportedException 
	 */
	@SuppressWarnings("deprecation")
	@Before
	public void setup() throws SQLException, ClassNotFoundException, CloneNotSupportedException{
		ConnectionState.valueOf(ConnectionState.NOP.toString()); // coverage BS.
		
		driver = new MockJDBCDriver(new MockJDBCAnswer() {
			
			public Connection answer() throws SQLException {
				return new MockConnection();
			}
		});

		hookClass = new CustomHook();
		mockConfig = createNiceMock(BoneCPConfig.class);
		expect(mockConfig.clone()).andReturn(mockConfig).anyTimes();
		
		expect(mockConfig.getPartitionCount()).andReturn(1).anyTimes();
		expect(mockConfig.getMaxConnectionsPerPartition()).andReturn(5).anyTimes();
		expect(mockConfig.getAcquireIncrement()).andReturn(0).anyTimes();
		expect(mockConfig.getMinConnectionsPerPartition()).andReturn(5).anyTimes();
		expect(mockConfig.getIdleConnectionTestPeriodInMinutes()).andReturn(10000L).anyTimes();
		expect(mockConfig.getUsername()).andReturn(CommonTestUtils.username).anyTimes();
		expect(mockConfig.getPassword()).andReturn(CommonTestUtils.password).anyTimes();
		expect(mockConfig.getJdbcUrl()).andReturn("jdbc:mock").anyTimes();
		expect(mockConfig.getReleaseHelperThreads()).andReturn(0).anyTimes();
		expect(mockConfig.isDisableConnectionTracking()).andReturn(true).anyTimes();
		expect(mockConfig.getConnectionHook()).andReturn(hookClass).anyTimes();
		replay(mockConfig);
	
		// once for no release threads, once with release threads....
		poolClass = new BoneCP(mockConfig);
	}
	
	/** Unload the driver.
	 * @throws SQLException
	 */
	@After
	public void destroy() throws SQLException{
		driver.disable();
		poolClass.shutdown();
		poolClass = null;
	}
	
	/**
	 * Test method for {@link com.jolbox.bonecp.hooks.AbstractConnectionHook#onAcquire(com.jolbox.bonecp.ConnectionHandle)}.
	 */
	@Test
	public void testOnAcquire() {
		assertEquals(5, hookClass.acquire);
	}


	/**
	 * Test method for {@link com.jolbox.bonecp.hooks.AbstractConnectionHook#onCheckOut(com.jolbox.bonecp.ConnectionHandle)}.
	 * @throws SQLException 
	 */
	@Test
	public void testOnCheckOutAndOnCheckin() throws SQLException {
		poolClass.getConnection().close();
		assertEquals(1, hookClass.checkout);
		assertEquals(1, hookClass.checkin);
	}

	/**
	 * Test method for {@link com.jolbox.bonecp.hooks.AbstractConnectionHook#onDestroy(com.jolbox.bonecp.ConnectionHandle)}.
	 */
	@Test
	public void testOnDestroy() {
		poolClass.close();
		assertEquals(5, hookClass.destroy);
	}

	
	/** Just to do code coverage of abstract class.
	 * @throws SQLException
	 * @throws CloneNotSupportedException 
	 */
	@SuppressWarnings("deprecation")
	@Test
	public void dummyCoverage() throws SQLException, CloneNotSupportedException{
		CoverageHook hook = new CoverageHook();
		reset(mockConfig);
		mockConfig.sanitize();
		expectLastCall().anyTimes();

		expect(mockConfig.getPartitionCount()).andReturn(1).anyTimes();
		expect(mockConfig.getMaxConnectionsPerPartition()).andReturn(5).anyTimes();
		expect(mockConfig.getMinConnectionsPerPartition()).andReturn(5).anyTimes();
		expect(mockConfig.getIdleConnectionTestPeriodInMinutes()).andReturn(10000L).anyTimes();
		expect(mockConfig.getUsername()).andReturn(CommonTestUtils.username).anyTimes();
		expect(mockConfig.getPassword()).andReturn(CommonTestUtils.password).anyTimes();
		expect(mockConfig.getJdbcUrl()).andReturn(CommonTestUtils.url).anyTimes();
		expect(mockConfig.getReleaseHelperThreads()).andReturn(0).anyTimes();
		expect(mockConfig.isDisableConnectionTracking()).andReturn(true).anyTimes();
		expect(mockConfig.getConnectionHook()).andReturn(hook).anyTimes();
		expect(mockConfig.clone()).andReturn(mockConfig).anyTimes();
		
		replay(mockConfig);
		
		poolClass = new BoneCP(mockConfig);	
		
		poolClass.getConnection().close();
		poolClass.close();
		
		reset(mockConfig);
		expect(mockConfig.getPartitionCount()).andReturn(1).anyTimes();
		expect(mockConfig.getMaxConnectionsPerPartition()).andReturn(5).anyTimes();
		expect(mockConfig.getMinConnectionsPerPartition()).andReturn(5).anyTimes();
		expect(mockConfig.getIdleConnectionTestPeriodInMinutes()).andReturn(10000L).anyTimes();
		expect(mockConfig.getUsername()).andReturn(CommonTestUtils.username).anyTimes();
		expect(mockConfig.getPassword()).andReturn(CommonTestUtils.password).anyTimes();
		expect(mockConfig.getReleaseHelperThreads()).andReturn(0).anyTimes();
		expect(mockConfig.getConnectionHook()).andReturn(hook).anyTimes();
		expect(mockConfig.isDisableConnectionTracking()).andReturn(true).anyTimes();
		expect(mockConfig.getJdbcUrl()).andReturn("something-bad").anyTimes();
		replay(mockConfig);
		try{
			poolClass = new BoneCP(mockConfig);
			// should throw an exception
		} catch (Exception e){
			// do nothing
		}
		
		poolClass.close();
	}
	
	/**
	 * Test method for {@link com.jolbox.bonecp.hooks.AbstractConnectionHook#onDestroy(com.jolbox.bonecp.ConnectionHandle)}.
	 * @throws SQLException 
	 * @throws CloneNotSupportedException 
	 */
	@SuppressWarnings("deprecation")
	@Test
	public void testOnAcquireFail() throws SQLException, CloneNotSupportedException {
		hookClass = new CustomHook();
		reset(mockConfig);
		mockConfig.sanitize();
		expectLastCall().anyTimes();

		driver.setConnection(null);
		driver.setMockJDBCAnswer(new MockJDBCAnswer() {
			
			public Connection answer() throws SQLException {
				throw new SQLException("Dummy error");
			}
		});
		expect(mockConfig.getPartitionCount()).andReturn(1).anyTimes();
		expect(mockConfig.getMaxConnectionsPerPartition()).andReturn(5).anyTimes();
		expect(mockConfig.getMinConnectionsPerPartition()).andReturn(5).anyTimes();
		expect(mockConfig.getIdleConnectionTestPeriodInMinutes()).andReturn(10000L).anyTimes();
		expect(mockConfig.getUsername()).andReturn(CommonTestUtils.username).anyTimes();
		expect(mockConfig.getPassword()).andReturn(CommonTestUtils.password).anyTimes();
		expect(mockConfig.getJdbcUrl()).andReturn("invalid").anyTimes();
		expect(mockConfig.getReleaseHelperThreads()).andReturn(0).anyTimes();
		expect(mockConfig.isDisableConnectionTracking()).andReturn(true).anyTimes();
		expect(mockConfig.getConnectionHook()).andReturn(hookClass).anyTimes();
		expect(mockConfig.clone()).andReturn(mockConfig).anyTimes();
		
		replay(mockConfig);
		
		try{
			poolClass = new BoneCP(mockConfig);	
			poolClass.getConnection();
		} catch (Exception e){
			// do nothing
		}
		assertEquals(1, hookClass.fail);
		
	}

	/**
	 * @throws SQLException
	 */
	@Test
	public void testOnAcquireFailDefault() throws SQLException {
		ConnectionHook hook = new AbstractConnectionHook() {
			// do nothing
		};
		AcquireFailConfig fail = createNiceMock(AcquireFailConfig.class);
		expect(fail.getAcquireRetryDelayInMs()).andReturn(0L).times(5).andThrow(new RuntimeException()).once();
		expect(fail.getAcquireRetryAttempts()).andReturn(new AtomicInteger(2)).times(3).andReturn(new AtomicInteger(1)).times(3);
		replay(fail);
		assertTrue(hook.onAcquireFail(new SQLException(), fail));
		assertFalse(hook.onAcquireFail(new SQLException(), fail));
		assertFalse(hook.onAcquireFail(new SQLException(), fail));
		
	}

	/**
	 * Test method.
	 * @throws SQLException 
	 * @throws CloneNotSupportedException 
	 */
	public void onQueryExecuteTimeLimitExceeded(boolean coverage) throws SQLException, CloneNotSupportedException {
		reset(mockConfig);
		expect(mockConfig.getPartitionCount()).andReturn(1).anyTimes();
		expect(mockConfig.getMaxConnectionsPerPartition()).andReturn(5).anyTimes();
		expect(mockConfig.getMinConnectionsPerPartition()).andReturn(5).anyTimes();
		expect(mockConfig.getIdleConnectionTestPeriodInMinutes()).andReturn(10000L).anyTimes();
		expect(mockConfig.getUsername()).andReturn(CommonTestUtils.username).anyTimes();
		expect(mockConfig.getPassword()).andReturn(CommonTestUtils.password).anyTimes();
		expect(mockConfig.getJdbcUrl()).andReturn("jdbc:mock").anyTimes();
		expect(mockConfig.isDisableConnectionTracking()).andReturn(true).anyTimes();
		expect(mockConfig.getConnectionHook()).andReturn(coverage ? new CoverageHook() : hookClass).anyTimes();
		expect(mockConfig.getQueryExecuteTimeLimitInMs()).andReturn(200L).anyTimes();
		expect(mockConfig.getConnectionTimeoutInMs()).andReturn(Long.MAX_VALUE).anyTimes();
		expect(mockConfig.isDeregisterDriverOnClose()).andReturn(false).anyTimes();
		expect(mockConfig.clone()).andReturn(mockConfig).anyTimes();
		
		PreparedStatement mockPreparedStatement = createNiceMock(PreparedStatement.class);
		Connection mockConnection = createNiceMock(Connection.class);
		expect(mockConnection.prepareStatement("")).andReturn(mockPreparedStatement).anyTimes();
		expect(mockPreparedStatement.execute()).andAnswer(new IAnswer<Boolean>() {
			
			public Boolean answer() throws Throwable {
				try {
					Thread.sleep(300); // something that exceeds our limit
				} catch(Exception e) {
				}
				return false;
			}
		}).once();
		driver.setConnection(mockConnection);
		replay(mockConfig, mockPreparedStatement, mockConnection);
		
			poolClass = new BoneCP(mockConfig);	
			Connection con = poolClass.getConnection();
			con.prepareStatement("").execute();
			
		reset(mockPreparedStatement, mockConnection);
		poolClass.close();
	}
	
	/**
	 * Test method.
	 * @throws SQLException 
	 * @throws CloneNotSupportedException 
	 */
	@Test
	public void testonQueryExecuteTimeLimitExceededCoverage() throws SQLException, CloneNotSupportedException {
		onQueryExecuteTimeLimitExceeded(true);
	}

	/**
	 * Test method.
	 * @throws SQLException 
	 * @throws CloneNotSupportedException 
	 */
	@Test
	public void testonQueryExecuteTimeLimitExceeded() throws SQLException, CloneNotSupportedException {
		onQueryExecuteTimeLimitExceeded(false);
		assertEquals(1, hookClass.queryTimeout);
		
	}

	
	public void markPossiblyBroken(boolean coverage) throws SQLException, CloneNotSupportedException{
			reset(mockConfig);
			expect(mockConfig.getPartitionCount()).andReturn(1).anyTimes();
			expect(mockConfig.getMaxConnectionsPerPartition()).andReturn(5).anyTimes();
			expect(mockConfig.getMinConnectionsPerPartition()).andReturn(5).anyTimes();
			expect(mockConfig.getIdleConnectionTestPeriodInMinutes()).andReturn(10000L).anyTimes();
			expect(mockConfig.getUsername()).andReturn(CommonTestUtils.username).anyTimes();
			expect(mockConfig.getPassword()).andReturn(CommonTestUtils.password).anyTimes();
			expect(mockConfig.getJdbcUrl()).andReturn("jdbc:mock").anyTimes();
			expect(mockConfig.isDisableConnectionTracking()).andReturn(true).anyTimes();
			expect(mockConfig.getConnectionHook()).andReturn(coverage ? new CoverageHook() : hookClass).anyTimes();
			expect(mockConfig.getQueryExecuteTimeLimitInMs()).andReturn(200L).anyTimes();
			expect(mockConfig.getConnectionTimeoutInMs()).andReturn(Long.MAX_VALUE).anyTimes();
			expect(mockConfig.isDeregisterDriverOnClose()).andReturn(false).anyTimes();
			expect(mockConfig.clone()).andReturn(mockConfig).anyTimes();
			
			PreparedStatement mockPreparedStatement = createNiceMock(PreparedStatement.class);
			Connection mockConnection = createNiceMock(Connection.class);
			expect(mockConnection.prepareStatement("")).andReturn(mockPreparedStatement).anyTimes();
			expect(mockPreparedStatement.execute()).andThrow(new SQLException("reason", "1234")).once();
			driver.setConnection(mockConnection);
			replay(mockConfig, mockPreparedStatement, mockConnection);
			
				poolClass = new BoneCP(mockConfig);	
				Connection con = poolClass.getConnection();
				try{
					con.prepareStatement("").execute();
					fail("Should throw exception");
				} catch (Exception e){
					// nothing
				}
				
				
			
			reset(mockPreparedStatement, mockConnection);
			poolClass.close();
	}
	

	@Test
	public void testOnMarkPossiblyBrokenCoverage() throws SQLException, CloneNotSupportedException{
		markPossiblyBroken(true);
	}

	@Test
	public void testOnMarkPossiblyBroken() throws SQLException, CloneNotSupportedException{
		markPossiblyBroken(false);
		assertEquals(1, hookClass.markPossiblyBroken);
	}

	
	public void connectionException(boolean coverage) throws SQLException, CloneNotSupportedException{
		reset(mockConfig);
		expect(mockConfig.getPartitionCount()).andReturn(1).anyTimes();
		expect(mockConfig.getMaxConnectionsPerPartition()).andReturn(5).anyTimes();
		expect(mockConfig.getMinConnectionsPerPartition()).andReturn(5).anyTimes();
		expect(mockConfig.getIdleConnectionTestPeriodInMinutes()).andReturn(10000L).anyTimes();
		expect(mockConfig.getUsername()).andReturn(CommonTestUtils.username).anyTimes();
		expect(mockConfig.getPassword()).andReturn(CommonTestUtils.password).anyTimes();
		expect(mockConfig.getJdbcUrl()).andReturn("jdbc:mock").anyTimes();
		expect(mockConfig.isDisableConnectionTracking()).andReturn(true).anyTimes();
		expect(mockConfig.getConnectionHook()).andReturn(coverage ? new CoverageHook() : hookClass).anyTimes();
		expect(mockConfig.getQueryExecuteTimeLimitInMs()).andReturn(200L).anyTimes();
		expect(mockConfig.getConnectionTimeoutInMs()).andReturn(Long.MAX_VALUE).anyTimes();
		expect(mockConfig.isDeregisterDriverOnClose()).andReturn(false).anyTimes();
		expect(mockConfig.clone()).andReturn(mockConfig).anyTimes();
		
		PreparedStatement mockPreparedStatement = createNiceMock(PreparedStatement.class);
		Connection mockConnection = createNiceMock(Connection.class);
		expect(mockConnection.prepareStatement("")).andReturn(mockPreparedStatement).anyTimes();
		expect(mockPreparedStatement.execute()).andThrow(new SQLException("reason", "8999")).once();
		driver.setConnection(mockConnection);
		replay(mockConfig, mockPreparedStatement, mockConnection);
		
			poolClass = new BoneCP(mockConfig);	
			Connection con = poolClass.getConnection();
			try{
				con.prepareStatement("").execute();
				fail("Should throw exception");
			} catch (Exception e){
				// nothing
			}
			
			
		
		reset(mockPreparedStatement, mockConnection);
		poolClass.close();
}


@Test
public void testConnectionExceptionCoverage() throws SQLException, CloneNotSupportedException{
	connectionException(true);
}

@Test
public void testOnConnectionException() throws SQLException, CloneNotSupportedException{
	connectionException(false);
	assertEquals(1, hookClass.connectionException);
}
	
}
