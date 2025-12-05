/*
  Function to render the footer content into the page
  This section dynamically generates the footer content for the web page, including the hospital's logo, copyright information, and various helpful links.
*/

function renderFooter() {
  // Select the footer element from the DOM
  const footer = document.getElementById("footer");
  
  // Check if footer element exists
  if (!footer) {
    console.warn("Footer element not found. Make sure you have an element with id='footer' in your HTML.");
    return;
  }
  
  // Set the inner HTML of the footer element to include the footer content
  footer.innerHTML = `
    <footer class="footer">
      <div class="footer-container">
        <!-- Hospital Logo and Copyright Info -->
        <div class="footer-logo">
          <img src="../assets/images/logo/logo.png" alt="Hospital CMS Logo" class="footer-logo-img">
          <p class="copyright-text">© Copyright 2025. All Rights Reserved by Hospital CMS.</p>
        </div>
        
        <!-- Links Section -->
        <div class="footer-links">
          <!-- Company Links Column -->
          <div class="footer-column">
            <h4 class="footer-heading">Company</h4>
            <a href="/pages/about.html" class="footer-link">About</a>
            <a href="/pages/careers.html" class="footer-link">Careers</a>
            <a href="/pages/press.html" class="footer-link">Press</a>
          </div>
          
          <!-- Support Links Column -->
          <div class="footer-column">
            <h4 class="footer-heading">Support</h4>
            <a href="/pages/account.html" class="footer-link">Account</a>
            <a href="/pages/help-center.html" class="footer-link">Help Center</a>
            <a href="/pages/contact.html" class="footer-link">Contact Us</a>
          </div>
          
          <!-- Legals Links Column -->
          <div class="footer-column">
            <h4 class="footer-heading">Legals</h4>
            <a href="/pages/terms.html" class="footer-link">Terms & Conditions</a>
            <a href="/pages/privacy.html" class="footer-link">Privacy Policy</a>
            <a href="/pages/licensing.html" class="footer-link">Licensing</a>
          </div>
        </div>
      </div>
    </footer>
  `;
  
  // Add click event listeners to footer links (optional enhancement)
  addFooterEventListeners();
}

// Optional helper function to handle footer link interactions
function addFooterEventListeners() {
  // Wait for DOM to update
  setTimeout(() => {
    const footerLinks = document.querySelectorAll('.footer-link');
    
    footerLinks.forEach(link => {
      link.addEventListener('click', (e) => {
        // You could add analytics tracking here
        console.log(`Footer link clicked: ${link.textContent} -> ${link.getAttribute('href')}`);
        
        // Optional: Add smooth scrolling for anchor links
        if (link.getAttribute('href').startsWith('#')) {
          e.preventDefault();
          const targetId = link.getAttribute('href').substring(1);
          const targetElement = document.getElementById(targetId);
          if (targetElement) {
            targetElement.scrollIntoView({ behavior: 'smooth' });
          }
        }
      });
    });
  }, 100);
}

// Call the renderFooter function when DOM is loaded
document.addEventListener('DOMContentLoaded', renderFooter);

// Also call renderFooter if using dynamic page loading
if (window.addEventListener) {
  window.addEventListener('load', renderFooter);
}

// Export for module usage if needed
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    renderFooter,
    addFooterEventListeners
  };
}