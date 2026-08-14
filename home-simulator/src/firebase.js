import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
  apiKey: "AIzaSyD27irr-LQXvqXRIedInZFTCIzV46BQs8A",
  authDomain: "smart-home-monitoring-madd.firebaseapp.com",
  projectId: "smart-home-monitoring-madd",
  storageBucket: "smart-home-monitoring-madd.firebasestorage.app",
  messagingSenderId: "670296192309",
  appId: "1:670296192309:web:409ca4695430bafa96887c",
  measurementId: "G-5P1Q7958M6"
};

// Initialize Firebase App
const app = initializeApp(firebaseConfig);

// Initialize Cloud Firestore
export const db = getFirestore(app);