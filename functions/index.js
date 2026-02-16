const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const {onDocumentUpdated} = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({ maxInstances: 10 });

// Auto delete empty group
exports.autoDeleteEmptyGroup = onDocumentUpdated("groups/{groupId}", async (event) => {
  const after = event.data.after.data();
  if (!after) return;

  if (!after.members || after.members.length === 0) {
    const groupId = event.params.groupId;
    const db = admin.firestore();

    console.log("Deleting empty group:", groupId);

    await admin.firestore().recursiveDelete(
      db.collection("groups").doc(groupId)
    );
  }
});
