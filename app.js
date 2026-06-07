// Pricing Estimator state
const choreCards = document.querySelectorAll('.chore-card');
const totalPriceEl = document.getElementById('total-price');

// Helper state variables (original values)
const originalHelper = {
  name: 'Aarti Sharma',
  img: 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=150&h=150',
  rating: '4.8 <span>(142 cleanings completed)</span>',
  status: 'Helper Dispatched',
  step1Desc: 'Aarti Sharma scheduled for 8:00 AM slot.',
  step2Title: 'On the way',
  step2Desc: 'Aarti is currently 10 mins away from your location.',
  step2Icon: 'fa-solid fa-location-dot'
};

const backupHelper = {
  name: 'Rajeshwari (Backup Helper)',
  img: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=150&h=150',
  rating: '4.9 <span>(218 cleanings completed)</span>',
  status: 'Backup Dispatched',
  step1Desc: 'Backup helper assigned automatically.',
  step2Title: 'Backup On the Way',
  step2Desc: 'Rajeshwari is on her way to your location (Arriving at 8:05 AM).',
  step2Icon: 'fa-solid fa-person-running'
};

// Select DOM elements for simulation
const btnTriggerLeave = document.getElementById('btn-trigger-leave');
const btnReset = document.getElementById('btn-reset');
const leaveAlert = document.getElementById('leave-alert');
const helperName = document.getElementById('helper-name');
const helperImg = document.getElementById('helper-img');
const helperRating = document.getElementById('helper-rating');
const scheduleStatus = document.getElementById('schedule-status');
const descAssigned = document.getElementById('desc-assigned');
const titleStatus = document.getElementById('title-status');
const descStatus = document.getElementById('desc-status');
const trackerStatusIcon = document.getElementById('tracker-status-icon');
const simLog = document.getElementById('sim-log');

let leaveTriggered = false;

// Pricing Calculation logic
function calculatePrice() {
  let total = 0;
  choreCards.forEach(card => {
    if (card.classList.contains('active')) {
      total += parseInt(card.getAttribute('data-price'));
    }
  });
  totalPriceEl.textContent = `₹${total.toLocaleString('en-IN')}/mo`;
}

// Attach listeners to chore cards
choreCards.forEach(card => {
  card.addEventListener('click', () => {
    card.classList.toggle('active');
    calculatePrice();
    logAction(`Chore selection updated: ${card.querySelector('h4').textContent}`);
  });
});

// Helper function to update logs
function logAction(message) {
  const timestamp = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  simLog.innerHTML = `[${timestamp}] ${message}<br>` + simLog.innerHTML;
}

// Trigger Leave Simulation logic
btnTriggerLeave.addEventListener('click', () => {
  if (leaveTriggered) {
    logAction('Leave simulation already running. Please click Reset to test again.');
    return;
  }
  
  leaveTriggered = true;
  logAction('<strong>[Alert]</strong> Aarti Sharma registered sudden leave at 07:45 AM.');
  logAction('<strong>[System]</strong> Initiating Backup Search Algorithm...');
  
  // Animate leaf card fading out momentarily to indicate change
  const helperCard = document.getElementById('helper-card');
  helperCard.style.opacity = '0.5';

  setTimeout(() => {
    // Show top banner alert
    leaveAlert.classList.add('show');
    
    // Update Helper Card to Backup
    helperImg.src = backupHelper.img;
    helperName.textContent = backupHelper.name;
    helperRating.innerHTML = backupHelper.rating;
    scheduleStatus.textContent = backupHelper.status;
    scheduleStatus.style.background = '#fef3c7'; // soft yellow for backup badge
    scheduleStatus.style.color = '#d97706';

    // Update timeline steps
    descAssigned.textContent = backupHelper.step1Desc;
    titleStatus.textContent = backupHelper.step2Title;
    descStatus.textContent = backupHelper.step2Desc;
    
    // Update step icon
    trackerStatusIcon.innerHTML = `<i class="${backupHelper.step2Icon}"></i>`;
    
    // Fade card back in
    helperCard.style.opacity = '1';
    helperCard.classList.add('fade-in');
    
    logAction(`<strong>[Match]</strong> Verified backup Rajeshwari assigned! Dispatched automatically.`);
    logAction(`<strong>[Success]</strong> Backup ETA: 8:05 AM (only 5 min delay). Zero-inconvenience maintained.`);
  }, 1200);
});

// Reset Simulation logic
btnReset.addEventListener('click', () => {
  leaveTriggered = false;
  leaveAlert.classList.remove('show');
  
  // Restore original helper
  helperImg.src = originalHelper.img;
  helperName.textContent = originalHelper.name;
  helperRating.innerHTML = originalHelper.rating;
  scheduleStatus.textContent = originalHelper.status;
  scheduleStatus.style.background = '#e6f2ff';
  scheduleStatus.style.color = '#0056b3';

  // Restore timeline steps
  descAssigned.textContent = originalHelper.step1Desc;
  titleStatus.textContent = originalHelper.step2Title;
  descStatus.textContent = originalHelper.step2Desc;
  
  // Restore step icon
  trackerStatusIcon.innerHTML = `<i class="${originalHelper.step2Icon}"></i>`;
  
  // Clear fade-in class
  document.getElementById('helper-card').classList.remove('fade-in');

  simLog.innerHTML = `[System Log] Dashboard reset. Daily tasks scheduled.`;
});
