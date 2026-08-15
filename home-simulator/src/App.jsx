import { useEffect, useState } from 'react';
import { db } from './firebase';
import { 
  collection, 
  onSnapshot, 
  doc, 
  updateDoc, 
  addDoc, 
  serverTimestamp 
} from 'firebase/firestore';

function App() {
  const [devices, setDevices] = useState([]);

  // Read real-time updates from the Firestore 'devices' collection
  useEffect(() => {
    const unsubscribe = onSnapshot(collection(db, 'devices'), (snapshot) => {
      const deviceList = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      setDevices(deviceList);
    });

    return () => unsubscribe();
  }, []);

  // Periodic Telemetry Logging (for Member 1's Reports feature)
  useEffect(() => {
    const interval = setInterval(() => {
      devices.forEach((device) => {
        // Log telemetry whenever a device status is ON
        if (device.status === 'ON') {
          addDoc(collection(db, 'usage_logs'), {
            deviceId: device.id,
            deviceName: device.name || device.id,
            type: device.type || 'N/A',
            powerWatts: device.powerWatts || 100, // Fallback default if not specified in Firestore
            timestamp: serverTimestamp()
          }).catch((err) => console.error("Telemetry write error:", err));
        }
      });
    }, 10000); // Sends logs every 10 seconds while device is ON

    return () => clearInterval(interval);
  }, [devices]);

  // Helper to toggle main power status (ON/OFF)
  const toggleDevice = async (deviceId, currentStatus) => {
    const newStatus = currentStatus === 'ON' ? 'OFF' : 'ON';
    const deviceRef = doc(db, 'devices', deviceId);
    await updateDoc(deviceRef, { status: newStatus });
  };

  // Helper to update specific fields in Firestore documents
  const updateDeviceData = async (deviceId, updatedFields) => {
    const deviceRef = doc(db, 'devices', deviceId);
    await updateDoc(deviceRef, updatedFields);
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'sans-serif', color: '#1a1a1a' }}>
  <h1 style={{ lineHeight: '1.2', marginBottom: '24px', color: '#ffffff' }}>
    Smart Home Hardware Simulator (Firestore Connected)
  </h1>
      
      <div style={{ display: 'grid', gap: '16px', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))' }}>
        {devices.map((device) => (
          <div 
  key={device.id} 
  style={{ 
    border: '1px solid #ccc', 
    borderRadius: '8px', 
    padding: '16px',
    color: '#222222', // Enforces dark, readable text
    backgroundColor: 
      device.status === 'ON' ? '#e8f5e9' : 
      device.status === 'ERROR' ? '#ffebee' : 
      device.status === 'DISCONNECTED' ? '#fff3e0' : '#ffffff'
  }}
>
            <h3>{device.name || device.id}</h3>
            <p><strong>Type:</strong> {device.type || 'N/A'}</p>
            <p><strong>Status:</strong> {device.status || 'OFF'}</p>

            {/* 1. MULTI-SWITCH UNIT (Gang box with multiple sub-switches) */}
            {device.type === 'MULTI_SWITCH' ? (
              <div style={{ marginTop: '10px' }}>
                <p><strong>Gang Box Switches:</strong></p>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
                  {((device.subSwitches && device.subSwitches.length ? device.subSwitches : [
                    { id: 1, name: 'Switch 1', status: 'OFF' },
                    { id: 2, name: 'Switch 2', status: 'OFF' },
                    { id: 3, name: 'Switch 3', status: 'OFF' }
                  ])).map((switchItem, idx) => {
                    const swState = switchItem.status === 'ON';
                    return (
                      <button 
                        key={switchItem.id ?? idx} 
                        onClick={() => {
                          const updatedSwitches = (device.subSwitches && device.subSwitches.length ? device.subSwitches : [
                            { id: 1, name: 'Switch 1', status: 'OFF' },
                            { id: 2, name: 'Switch 2', status: 'OFF' },
                            { id: 3, name: 'Switch 3', status: 'OFF' }
                          ]).map((item, i) => i === idx ? { ...item, status: swState ? 'OFF' : 'ON' } : item);
                          updateDeviceData(device.id, { subSwitches: updatedSwitches });
                        }}
                        style={{ 
                          padding: '6px 12px',
                          cursor: 'pointer',
                          borderRadius: '4px',
                          border: '1px solid #999',
                          backgroundColor: swState ? '#4caf50' : '#e0e0e0',
                          color: swState ? '#fff' : '#000'
                        }}
                      >
                        {switchItem.name || `Switch ${idx + 1}`}: {swState ? 'ON' : 'OFF'}
                      </button>
                    );
                  })}
                </div>
              </div>

            /* 2. SECURITY CAMERA (Mock feed and disconnection simulator) */
            ) : device.type === 'CAMERA' ? (
              <div style={{ marginTop: '10px' }}>
                <div style={{ position: 'relative', width: '100%', height: '140px', backgroundColor: '#000', borderRadius: '4px', overflow: 'hidden' }}>
                  {device.status !== 'DISCONNECTED' ? (
                    <img 
                      src={device.streamUrl || "https://picsum.photos/300/180"} 
                      alt="Camera Feed" 
                      style={{ width: '100%', height: '100%', objectFit: 'cover', opacity: device.status === 'ON' ? 1 : 0.2 }}
                    />
                  ) : (
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#ff9800' }}>
                      NO SIGNAL (DISCONNECTED)
                    </div>
                  )}
                </div>
                <div style={{ marginTop: '10px', display: 'flex', gap: '8px' }}>
                  <button onClick={() => toggleDevice(device.id, device.status)}>
                    Toggle Power
                  </button>
                  <button 
                    onClick={() => updateDeviceData(device.id, { 
                      status: device.status === 'DISCONNECTED' ? 'OFF' : 'DISCONNECTED' 
                    })}
                    style={{ backgroundColor: '#ff9800', color: '#fff', border: 'none', padding: '6px 10px', borderRadius: '4px', cursor: 'pointer' }}
                  >
                    {device.status === 'DISCONNECTED' ? 'Reconnect' : 'Simulate Disconnect'}
                  </button>
                </div>
              </div>

            /* 3. SAFETY-CRITICAL APPLIANCE (Iron with timer & fault simulator) */
            ) : device.type === 'IRON' ? (
              <div style={{ marginTop: '10px' }}>
                <p><strong>Max Duration:</strong> {device.maxOnDuration || 900}s</p>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button onClick={() => toggleDevice(device.id, device.status)}>
                    Toggle Power
                  </button>
                  <button 
                    onClick={() => updateDeviceData(device.id, { status: 'ERROR' })}
                    style={{ backgroundColor: '#f44336', color: '#fff', border: 'none', padding: '6px 10px', borderRadius: '4px', cursor: 'pointer' }}
                  >
                    Simulate Overheat Fault
                  </button>
                </div>
              </div>

            /* 4. STANDARD SINGLE-NODE BINARY (Outlets & Lights) */
            ) : (
              <button onClick={() => toggleDevice(device.id, device.status)} style={{ marginTop: '10px' }}>
                Toggle Power
              </button>
            )}
          </div>
        ))}
      </div>

      {devices.length === 0 && (
        <p>No devices found in the 'devices' Firestore collection yet.</p>
      )}
    </div>
  );
}

export default App;