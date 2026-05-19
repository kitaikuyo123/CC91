import React, { useContext } from 'react';
import { Link as RouterLink, UNSAFE_NavigationContext } from 'react-router-dom';

interface SafeLinkProps extends React.AnchorHTMLAttributes<HTMLAnchorElement> {
  to: any;
  reloadDocument?: boolean;
  replace?: boolean;
  state?: any;
}

export default function SafeLink({ to, children, ...props }: SafeLinkProps) {
  const context = useContext(UNSAFE_NavigationContext);
  const inRouter = context !== null && context !== undefined;

  if (inRouter) {
    return (
      <RouterLink to={to} {...(props as any)}>
        {children}
      </RouterLink>
    );
  }

  const href = typeof to === 'string' ? to : (to?.pathname || '');
  return (
    <a href={href} {...props}>
      {children}
    </a>
  );
}
