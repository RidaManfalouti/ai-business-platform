import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { useAuth } from '@/context/AuthContext';
import { Clock } from 'lucide-react';

export const AppLayout: React.FC = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { isSessionExpiringSoon, sessionRemainingSeconds, expireSessionNow } = useAuth();

  return (
    <div className="flex h-screen overflow-hidden bg-[#09090B] text-[#FAFAFA]">
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex flex-1 flex-col overflow-hidden bg-[#09090B]">
        <Header onToggleSidebar={() => setSidebarOpen((prev) => !prev)} />

        {/* Warning banner when session expires in less than 5 minutes */}
        {isSessionExpiringSoon && (
          <div className="bg-amber-950/80 border-b border-amber-800/80 px-4 py-2 flex items-center justify-between text-xs text-amber-200">
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-amber-400 shrink-0 animate-pulse" />
              <span>
                <strong>Session expirant bientôt :</strong> Votre session expirera dans moins de 5 minutes ({Math.floor((sessionRemainingSeconds || 0) / 60)}m {(sessionRemainingSeconds || 0) % 60}s).
              </span>
            </div>
            <button
              type="button"
              onClick={expireSessionNow}
              className="text-amber-300 underline hover:text-white font-medium ml-4 shrink-0 transition-colors cursor-pointer"
            >
              Se reconnecter
            </button>
          </div>
        )}

        <main className="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8 bg-[#09090B]">
          <div className="mx-auto max-w-7xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
};
