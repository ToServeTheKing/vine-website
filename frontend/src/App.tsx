import AdminPage from '@/pages/Admin';

/**
 * What's left of the React app: the admin screen, and nothing else.
 *
 * The shop front is server-rendered Thymeleaf now, so there are no client-side routes to route
 * between — react-router went with the pages it used to switch. Spring serves this shell at /admin and
 * only at /admin; every other URL is a page in src/main/resources/templates.
 *
 * The admin sits outside the public chrome deliberately: the nav would offer a signed-in editor links
 * away from unsaved work, and the opening hours in the footer are noise on a screen whose whole job is
 * the catalogue.
 */
const App = () => (
  <div className="min-h-screen bg-bakery-50">
    <AdminPage />
  </div>
);

export default App;
