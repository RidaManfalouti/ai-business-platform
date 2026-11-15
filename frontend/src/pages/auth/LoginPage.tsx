import React, { useState } from 'react';
import { useNavigate, useLocation, Link, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/context/AuthContext';
import { Input } from '@/components/common/Input';
import { Button } from '@/components/common/Button';
import { Alert } from '@/components/common/Alert';
import { Layers, Lock, Mail, ArrowRight, ShieldAlert } from 'lucide-react';
import { ParsedApiError } from '@/types/api';

const loginSchema = z.object({
  email: z.string().min(1, 'L’adresse email est obligatoire').email('Format d’adresse email invalide'),
  password: z.string().min(1, 'Le mot de passe est obligatoire'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const [serverError, setServerError] = useState<string | null>(null);

  const isExpired = searchParams.get('expired') === 'true';
  const from = (location.state as { from?: { pathname: string } })?.from?.pathname || '/';

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    setServerError(null);
    try {
      await login(data);
      navigate(from, { replace: true });
    } catch (err: unknown) {
      const parsed = err as ParsedApiError;
      setServerError(parsed.message || 'Identifiants invalides ou service injoignable.');
    }
  };

  return (
    <div className="min-h-screen bg-[#09090B] flex flex-col justify-center py-12 sm:px-6 lg:px-8 relative overflow-hidden">
      {/* Ambient background glow */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[600px] h-[350px] bg-gradient-to-b from-[#A855F7]/15 via-[#FACC15]/5 to-transparent blur-3xl pointer-events-none" />

      <div className="sm:mx-auto sm:w-full sm:max-w-md relative z-10">
        <div className="flex justify-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#FACC15] text-[#09090B] font-black shadow-[0_0_25px_rgba(250,204,21,0.35)]">
            <Layers className="w-7 h-7" />
          </div>
        </div>
        <h2 className="mt-4 text-center text-2xl font-bold tracking-tight text-[#FAFAFA]">
          AI Business Platform
        </h2>
        <p className="mt-1 text-center text-xs text-[#A1A1AA]">
          Plateforme de gestion commerciale microservices pilotée par l'IA
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md relative z-10">
        <div className="bg-[#18181B] py-8 px-4 shadow-2xl sm:rounded-2xl sm:px-10 border border-[#27272A] shadow-[0_25px_60px_rgba(0,0,0,0.8)]">
          <h3 className="text-lg font-bold text-[#FAFAFA] mb-6">Connexion à votre espace</h3>

          {isExpired && (
            <div className="mb-5">
              <Alert variant="warning" title="Session expirée">
                Votre session a expiré pour des raisons de sécurité. Veuillez vous reconnecter.
              </Alert>
            </div>
          )}

          {serverError && (
            <div className="mb-5">
              <Alert variant="error" title="Échec de connexion">
                {serverError}
              </Alert>
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Input
              label="Adresse email"
              type="email"
              placeholder="ex: jean.dupont@example.com"
              leftIcon={<Mail className="w-4 h-4" />}
              error={errors.email?.message}
              required
              {...register('email')}
            />

            <Input
              label="Mot de passe"
              type="password"
              placeholder="••••••••"
              leftIcon={<Lock className="w-4 h-4" />}
              error={errors.password?.message}
              required
              {...register('password')}
            />

            <div className="pt-2">
              <Button
                type="submit"
                variant="primary"
                size="md"
                className="w-full"
                isLoading={isSubmitting}
                icon={<ArrowRight className="w-4 h-4" />}
              >
                Se connecter
              </Button>
            </div>
          </form>

          <div className="mt-6 pt-6 border-t border-[#27272A] text-center">
            <p className="text-xs text-[#A1A1AA]">
              Pas encore de compte ?{' '}
              <Link to="/register" className="font-semibold text-[#FACC15] hover:text-[#EAB308] transition-colors">
                Créer un compte
              </Link>
            </p>
          </div>

          {/* Quick connection tip */}
          <div className="mt-6 p-3.5 bg-[#202024]/60 rounded-xl border border-[#27272A] text-xs text-[#A1A1AA] space-y-1">
            <div className="font-semibold flex items-center gap-1.5 text-[#FAFAFA]">
              <ShieldAlert className="w-3.5 h-3.5 text-[#FACC15]" />
              <span>Authentification JWT & Passerelle</span>
            </div>
            <p className="text-[11px] text-[#71717A]">
              Vous pouvez vous connecter avec votre compte utilisateur ou créer un nouveau compte via
              le lien ci-dessus.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
